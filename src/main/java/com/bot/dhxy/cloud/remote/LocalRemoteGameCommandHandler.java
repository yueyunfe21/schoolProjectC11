package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.service.AutoCombatPanelService;
import com.bot.dhxy.service.SummonSkillService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.LeftTopStatusSwitchService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RunningTaskHandle;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;
import com.bot.dhxy.service.dialog.DialogDetectionLocalMechanics;
import com.bot.dhxy.service.dialog.DialogOptionOcrImageLocalObservationMechanics;
import com.bot.dhxy.service.dialog.DialogOptionOcrWordsLocalObservationMechanics;
import com.bot.dhxy.service.dialog.DialogPreparedActionValidationLocalMechanics;
import com.bot.dhxy.service.dialog.DialogWhiteStoryTemplateLocalObservationMechanics;
import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.service.bag.BagReturnItemMacroResult;
import com.bot.dhxy.service.battleradar.BattleRadarLocalObservationMechanics;
import com.bot.dhxy.service.commonbox.CommonBoxLocalObservationMechanics;
import com.bot.dhxy.service.tasktracker.TaskTrackerPanelRectLocalObservationMechanics;
import com.bot.dhxy.service.teamreturn.TeamReturnButtonLocalObservationMechanics;
import com.bot.dhxy.service.teamreturn.TeamReturnLeaderSignalLocalObservationMechanics;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Local mechanical executor for cloud commands.
 *
 * <p>This adapter validates explicit run/window bindings and performs only capture, window-fact
 * reads, one atomic input-bundle submission, and the dormant retained summon-skill whole pass.
 * The whole pass delegates to the existing local service and adds no OCR, click, retry, fallback,
 * or task-phase decisions.</p>
 */
public final class LocalRemoteGameCommandHandler implements RemoteCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(LocalRemoteGameCommandHandler.class);

    private final RemoteClientSessionRef clientSession;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final BoundWindowCaptureService captureService;
    private final WindowFocusService windowFocusService;
    private final InputActionQueue inputActionQueue;
    private final RemoteTaskRunRegistry taskRunRegistry;
    private final RemoteOperationLedger operationLedger;
    private final SummonSkillService summonSkillService;
    private final UICleanerService uiCleanerService;
    private final LeftTopStatusSwitchService leftTopStatusSwitchService;
    private final AutoCombatPanelService autoCombatPanelService;
    private final CommonBoxLocalObservationMechanics commonBoxLocalObservationMechanics;
    private final TeamReturnButtonLocalObservationMechanics teamReturnButtonLocalObservationMechanics;
    private final TeamReturnLeaderSignalLocalObservationMechanics teamReturnLeaderSignalLocalObservationMechanics;
    private final TaskTrackerPanelRectLocalObservationMechanics taskTrackerPanelRectLocalObservationMechanics;
    private final BattleRadarLocalObservationMechanics battleRadarLocalObservationMechanics;
    private final BagService bagService;
    private final NavigationService navigationService;
    private final DialogDetectionLocalMechanics dialogDetectionLocalMechanics;
    private final PlayerStateFirstAidLocalMacroMechanics playerStateFirstAidLocalMacroMechanics;
    private final DialogPreparedActionValidationLocalMechanics dialogPreparedActionValidationLocalMechanics;
    private final DialogOptionOcrImageLocalObservationMechanics dialogOptionOcrImageLocalObservationMechanics;
    private final DialogOptionOcrWordsLocalObservationMechanics dialogOptionOcrWordsLocalObservationMechanics;
    private final DialogWhiteStoryTemplateLocalObservationMechanics dialogWhiteStoryTemplateLocalObservationMechanics;
    private final RemoteOperationPayloadCodec payloadCodec = new RemoteOperationPayloadCodec();
    private final RemoteInputActionMapper inputActionMapper = new RemoteInputActionMapper();
    private final RemoteProtocolDigests protocolDigests = new RemoteProtocolDigests();

    /**
     * Creates an inert, explicitly injectable mechanical command handler.
     *
     * @param clientSession tenant/device/session identity of the owning polling loop
     * @param multiWindowTaskManager exact logical-window runner registry
     * @param windowTaskContextHolder temporary thread binding used for one input bundle submission
     * @param bindingRefreshService exact-HWND live binding refresher; it never searches by title
     * @param captureService exact-HWND bitmap capture provider
     * @param windowFocusService read-only foreground HWND source for FOCUS_STATE facts
     * @param inputActionQueue existing globally serialized physical input queue
     * @param taskRunRegistry explicit remote task-run safety registry
     * @param operationLedger in-memory idempotency and input action-id ledger
     * @param summonSkillService existing whole-pass workflow; invoked only on the input worker
     * @param uiCleanerService existing deterministic post-pass cleaner
     * @param leftTopStatusSwitchService existing read-only left-top status fact producer
     * @param autoCombatPanelService existing read-only auto-combat panel fact producer
     */
    public LocalRemoteGameCommandHandler(
            RemoteClientSessionRef clientSession,
            MultiWindowTaskManager multiWindowTaskManager,
            WindowTaskContextHolder windowTaskContextHolder,
            WindowNativeBindingRefreshService bindingRefreshService,
            BoundWindowCaptureService captureService,
            WindowFocusService windowFocusService,
            InputActionQueue inputActionQueue,
            RemoteTaskRunRegistry taskRunRegistry,
            RemoteOperationLedger operationLedger,
            SummonSkillService summonSkillService,
            UICleanerService uiCleanerService,
            LeftTopStatusSwitchService leftTopStatusSwitchService,
            AutoCombatPanelService autoCombatPanelService,
            CommonBoxLocalObservationMechanics commonBoxLocalObservationMechanics,
            TeamReturnButtonLocalObservationMechanics teamReturnButtonLocalObservationMechanics,
            TeamReturnLeaderSignalLocalObservationMechanics teamReturnLeaderSignalLocalObservationMechanics,
            TaskTrackerPanelRectLocalObservationMechanics taskTrackerPanelRectLocalObservationMechanics,
            BattleRadarLocalObservationMechanics battleRadarLocalObservationMechanics,
            BagService bagService,
            NavigationService navigationService,
            DialogDetectionLocalMechanics dialogDetectionLocalMechanics,
            PlayerStateFirstAidLocalMacroMechanics playerStateFirstAidLocalMacroMechanics,
            DialogPreparedActionValidationLocalMechanics dialogPreparedActionValidationLocalMechanics,
            DialogOptionOcrImageLocalObservationMechanics dialogOptionOcrImageLocalObservationMechanics,
            DialogOptionOcrWordsLocalObservationMechanics dialogOptionOcrWordsLocalObservationMechanics,
            DialogWhiteStoryTemplateLocalObservationMechanics dialogWhiteStoryTemplateLocalObservationMechanics) {
        this.clientSession = RemoteClientSessionRef.builder()
                .tenantId(requireText(clientSession == null ? null : clientSession.getTenantId(), "tenantId"))
                .userId(requireText(clientSession == null ? null : clientSession.getUserId(), "userId"))
                .deviceId(requireText(clientSession == null ? null : clientSession.getDeviceId(), "deviceId"))
                .clientSessionId(requireText(
                        clientSession == null ? null : clientSession.getClientSessionId(),
                        "clientSessionId"))
                .build();
        this.multiWindowTaskManager = Objects.requireNonNull(multiWindowTaskManager, "multiWindowTaskManager");
        this.windowTaskContextHolder = Objects.requireNonNull(windowTaskContextHolder, "windowTaskContextHolder");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.windowFocusService = Objects.requireNonNull(windowFocusService, "windowFocusService");
        this.inputActionQueue = Objects.requireNonNull(inputActionQueue, "inputActionQueue");
        this.taskRunRegistry = Objects.requireNonNull(taskRunRegistry, "taskRunRegistry");
        this.operationLedger = Objects.requireNonNull(operationLedger, "operationLedger");
        this.summonSkillService = Objects.requireNonNull(summonSkillService, "summonSkillService");
        this.uiCleanerService = Objects.requireNonNull(uiCleanerService, "uiCleanerService");
        this.leftTopStatusSwitchService = Objects.requireNonNull(
                leftTopStatusSwitchService, "leftTopStatusSwitchService");
        this.autoCombatPanelService = Objects.requireNonNull(
                autoCombatPanelService, "autoCombatPanelService");
        this.commonBoxLocalObservationMechanics = Objects.requireNonNull(
                commonBoxLocalObservationMechanics, "commonBoxLocalObservationMechanics");
        this.teamReturnButtonLocalObservationMechanics = Objects.requireNonNull(
                teamReturnButtonLocalObservationMechanics, "teamReturnButtonLocalObservationMechanics");
        this.teamReturnLeaderSignalLocalObservationMechanics = Objects.requireNonNull(
                teamReturnLeaderSignalLocalObservationMechanics, "teamReturnLeaderSignalLocalObservationMechanics");
        this.taskTrackerPanelRectLocalObservationMechanics = Objects.requireNonNull(
                taskTrackerPanelRectLocalObservationMechanics, "taskTrackerPanelRectLocalObservationMechanics");
        this.battleRadarLocalObservationMechanics = Objects.requireNonNull(
                battleRadarLocalObservationMechanics, "battleRadarLocalObservationMechanics");
        this.bagService = Objects.requireNonNull(bagService, "bagService");
        this.navigationService = Objects.requireNonNull(navigationService, "navigationService");
        this.dialogDetectionLocalMechanics = Objects.requireNonNull(
                dialogDetectionLocalMechanics, "dialogDetectionLocalMechanics");
        this.playerStateFirstAidLocalMacroMechanics = Objects.requireNonNull(
                playerStateFirstAidLocalMacroMechanics, "playerStateFirstAidLocalMacroMechanics");
        this.dialogPreparedActionValidationLocalMechanics = Objects.requireNonNull(
                dialogPreparedActionValidationLocalMechanics, "dialogPreparedActionValidationLocalMechanics");
        this.dialogOptionOcrImageLocalObservationMechanics = Objects.requireNonNull(
                dialogOptionOcrImageLocalObservationMechanics, "dialogOptionOcrImageLocalObservationMechanics");
        this.dialogOptionOcrWordsLocalObservationMechanics = Objects.requireNonNull(
                dialogOptionOcrWordsLocalObservationMechanics, "dialogOptionOcrWordsLocalObservationMechanics");
        this.dialogWhiteStoryTemplateLocalObservationMechanics = Objects.requireNonNull(
                dialogWhiteStoryTemplateLocalObservationMechanics,
                "dialogWhiteStoryTemplateLocalObservationMechanics");
    }

    /**
     * Executes at most one correlated mechanical operation and always returns a terminal outcome.
     *
     * @param command validated transport envelope; non-null and contract version 1
     * @return correlated terminal outcome, including conservative UNKNOWN for ambiguous input failure
     */
    @Override
    public RemoteGameOutcomeEnvelope handle(RemoteGameCommand command) {
        Objects.requireNonNull(command, "command");
        OperationTiming timing = OperationTiming.start(command.getTimeoutMs());
        boolean declaredGenericSessionStep = command.getPayload() != null
                && command.getPayload().isObject()
                && command.getPayload().hasNonNull("sessionRef")
                && (command.getOperation() == RemoteGameOperation.CAPTURE
                        || command.getOperation()
                                == RemoteGameOperation.EXECUTE_INPUT_BUNDLE);

        try {
            if (!protocolDigests.requestDigestMatches(command)) {
                return terminal(
                        command,
                        matchingRegistration(command),
                        declaredGenericSessionStep
                                ? RemoteExecutionState.UNKNOWN
                                : RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.INVALID_REQUEST,
                        "requestDigest does not match typed request",
                        timing,
                        emptyOutcomePayload(command));
            }
        } catch (RuntimeException e) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    declaredGenericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    "requestDigest validation failed: " + safeMessage(e),
                    timing,
                    emptyOutcomePayload(command));
        }

        Object decodedPayload;
        try {
            decodedPayload = switch (command.getOperation()) {
                case CAPTURE -> payloadCodec.readCapture(command.getPayload());
                case WINDOW_FACT -> payloadCodec.readWindowFact(command.getPayload());
                case EXECUTE_INPUT_BUNDLE -> payloadCodec.readInputBundle(command.getPayload());
                case EXCLUSIVE_INTERACTION_CONTROL ->
                        payloadCodec.readExclusiveInteractionControl(command.getPayload());
                case SUMMON_SKILL_WHOLE_PASS ->
                        payloadCodec.readSummonSkillWholePass(command.getPayload());
                case TASK_TRACKER_READ -> payloadCodec.readTaskTrackerRead(command.getPayload());
                case TASK_TRACKER_MATERIALIZE_ACTION ->
                        payloadCodec.readTaskTrackerMaterialize(command);
                case LOCAL_MACRO -> payloadCodec.readLocalMacro(command.getPayload());
            };
        } catch (RemotePayloadException | IllegalArgumentException e) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    declaredGenericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    "invalid operation payload: " + safeMessage(e),
                    timing,
                    emptyOutcomePayload(command));
        }

        if (command.getOperation() == RemoteGameOperation.TASK_TRACKER_READ
                || command.getOperation()
                        == RemoteGameOperation.TASK_TRACKER_MATERIALIZE_ACTION) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    "task tracker operation is dormant and unsupported by the local handler",
                    timing,
                    emptyOutcomePayload(command));
        }
        // A consumable generic final is forbidden until the matching local cursor is reserved.
        boolean genericSessionStep =
                (decodedPayload instanceof RemoteCaptureCommandPayload capture
                        && capture.getSessionRef() != null)
                        || (decodedPayload instanceof RemoteInputBundleCommandPayload input
                                && input.getSessionRef() != null);

        RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot =
                taskRunRegistry.commandAdmissionSnapshot(clientSession, command);
        RemoteOperationLedger.Claim claim;
        try {
            claim = operationLedger.claim(command, admissionSnapshot);
        } catch (RuntimeException e) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INTERNAL_ERROR,
                    "operation ledger claim failed: " + safeMessage(e),
                    timing,
                    emptyOutcomePayload(command));
        }

        if (claim.getStatus() == RemoteOperationLedger.ClaimStatus.DUPLICATE) {
            try {
                return claim.awaitTerminalOutcome();
            } catch (RuntimeException e) {
                return terminal(
                        command,
                        matchingRegistration(command),
                        RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.INTERNAL_ERROR,
                        "duplicate outcome wait failed: " + safeMessage(e),
                        timing,
                        emptyOutcomePayload(command));
            }
        }
        if (claim.getStatus() == RemoteOperationLedger.ClaimStatus.IDEMPOTENCY_CONFLICT) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.IDEMPOTENCY_CONFLICT,
                    "requestId was already used with another requestDigest",
                    timing,
                    emptyOutcomePayload(command));
        }
        if (claim.getStatus() == RemoteOperationLedger.ClaimStatus.ACTION_ID_REUSE) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.ACTION_ID_REUSE,
                    "input actionId was already used by another request",
                    timing,
                    emptyOutcomePayload(command));
        }
        if (claim.getStatus() == RemoteOperationLedger.ClaimStatus.FINAL_CONSUMED) {
            return terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.FINAL_CONSUMED,
                    "semantic action attempt is already below the local compacted frontier",
                    timing,
                    emptyOutcomePayload(command));
        }
        if (claim.getStatus() != RemoteOperationLedger.ClaimStatus.OWNER) {
            RemoteOutcomeCode rejectionCode = switch (claim.getStatus()) {
                case TASK_RUN_MISMATCH -> RemoteOutcomeCode.TASK_RUN_MISMATCH;
                case WRONG_WINDOW -> RemoteOutcomeCode.WRONG_WINDOW;
                case CAPACITY_EXCEEDED -> RemoteOutcomeCode.BROKER_CAPACITY_EXCEEDED;
                case COORDINATED_RESTART_REQUIRED -> RemoteOutcomeCode.CLIENT_RESTARTED;
                case SEMANTIC_CONFLICT -> RemoteOutcomeCode.IDEMPOTENCY_CONFLICT;
                default -> RemoteOutcomeCode.INTERNAL_ERROR;
            };
            return terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    rejectionCode,
                    "operation ledger rejected semantic admission: " + claim.getStatus(),
                    timing,
                    emptyOutcomePayload(command));
        }

        if (!taskRunRegistry.isCurrent(admissionSnapshot, command)) {
            RemoteGameOutcomeEnvelope stale = terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TASK_RUN_MISMATCH,
                    "task-run registration generation changed after ledger admission",
                    timing,
                    emptyOutcomePayload(command));
            operationLedger.complete(claim, stale);
            return stale;
        }

        RemoteGameOutcomeEnvelope outcome;
        try {
            outcome = executeOwnedCommand(
                    command, decodedPayload, admissionSnapshot, timing);
        } catch (TerminalSignal e) {
            outcome = terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep ? RemoteExecutionState.UNKNOWN : e.executionState,
                    e.code,
                    genericSessionStep
                            ? "generic step failed before local cursor reservation: "
                                    + e.getMessage()
                            : e.getMessage(),
                    timing,
                    emptyOutcomePayload(command));
        } catch (RemotePayloadException | IllegalArgumentException e) {
            outcome = terminal(
                    command,
                    matchingRegistration(command),
                    genericSessionStep
                            ? RemoteExecutionState.UNKNOWN
                            : RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    (genericSessionStep
                            ? "generic step failed before local cursor reservation: "
                            : "invalid operation payload: ") + safeMessage(e),
                    timing,
                    emptyOutcomePayload(command));
        } catch (Exception e) {
            outcome = terminal(
                    command,
                    matchingRegistration(command),
                    RemoteExecutionState.UNKNOWN,
                    RemoteOutcomeCode.INTERNAL_ERROR,
                    "mechanical handler failure: " + safeMessage(e),
                    timing,
                    emptyOutcomePayload(command));
        }

        try {
            operationLedger.complete(claim, outcome);
        } catch (RuntimeException e) {
            log.error(
                    "Remote operation ledger completion failed: requestId={} actionId={} taskRunId={} reason={}",
                    command.getRequestId(),
                    command.getActionId(),
                    command.getTaskRunId(),
                    e.getMessage());
        }
        return outcome;
    }

    private RemoteGameOutcomeEnvelope executeOwnedCommand(
            RemoteGameCommand command,
            Object decodedPayload,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            OperationTiming timing) throws Exception {
        if (command.getOperation() == RemoteGameOperation.TASK_TRACKER_READ
                || command.getOperation()
                        == RemoteGameOperation.TASK_TRACKER_MATERIALIZE_ACTION) {
            throw new IllegalStateException(
                    "dormant task tracker operation reached executeOwnedCommand");
        }
        RemoteTaskRunRegistration registration = requireRegistration(command, null, false);
        BindingAccess access = requireBoundWindow(
                command,
                isPhysicalInputOperation(command.getOperation()));
        registration = requireRegistration(command, access.runner(), true);
        boolean sessionBoundMechanical = switch (command.getOperation()) {
            case CAPTURE -> ((RemoteCaptureCommandPayload) decodedPayload).getSessionRef() != null;
            case EXECUTE_INPUT_BUNDLE ->
                    ((RemoteInputBundleCommandPayload) decodedPayload).getSessionRef() != null;
            default -> false;
        };
        if (timing.timedOut() && !sessionBoundMechanical) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TIMEOUT,
                    "command timeout elapsed before operation start");
        }

        return switch (command.getOperation()) {
            case CAPTURE -> executeCapture(
                    command, (RemoteCaptureCommandPayload) decodedPayload, admissionSnapshot,
                    registration, access, timing);
            case WINDOW_FACT -> executeWindowFact(
                    command, (RemoteWindowFactCommandPayload) decodedPayload, registration, access, timing);
            case EXECUTE_INPUT_BUNDLE -> executeInputBundle(
                    command, (RemoteInputBundleCommandPayload) decodedPayload, admissionSnapshot,
                    registration, access, timing);
            case EXCLUSIVE_INTERACTION_CONTROL -> executeExclusiveInteractionControl(
                    command,
                    (RemoteExclusiveInteractionControlCommandPayload) decodedPayload,
                    admissionSnapshot, registration, access, timing);
            case SUMMON_SKILL_WHOLE_PASS -> executeSummonSkillWholePass(
                    command,
                    (RemoteSummonSkillWholePassCommandPayload) decodedPayload,
                    admissionSnapshot,
                    registration,
                    access,
                    timing);
            case TASK_TRACKER_READ -> throw new IllegalStateException(
                    "TASK_TRACKER_READ is dormant and cannot execute");
            case TASK_TRACKER_MATERIALIZE_ACTION -> throw new IllegalStateException(
                    "TASK_TRACKER_MATERIALIZE_ACTION is dormant and cannot execute");
            case LOCAL_MACRO -> executeLocalMacro(
                    command, (RemoteLocalMacroCommandPayload) decodedPayload, admissionSnapshot,
                    registration, access, timing);
        };
    }

    private RemoteGameOutcomeEnvelope executeCapture(
            RemoteGameCommand command,
            RemoteCaptureCommandPayload request,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) throws Exception {
        RemoteTaskRunRegistry.InFlightExclusiveHandle genericHandle = null;
        if (request.getSessionRef() != null) {
            try {
                genericHandle = taskRunRegistry.bindGenericExclusiveStep(
                        admissionSnapshot, command, request.getSessionRef());
            } catch (IllegalStateException staleSession) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture session fence rejected before execution: "
                                + staleSession.getMessage(),
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
        }
        if (genericHandle != null && timing.timedOut()) {
            try {
                taskRunRegistry.completeGenericStep(
                        genericHandle, request.getSessionRef(), true);
            } catch (RuntimeException staleTimedOutStep) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture timed out before frame but lost its exact cursor",
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
            return terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TIMEOUT,
                    "command timeout elapsed before generic capture frame",
                    timing, emptyCapturePayload(request.getCaptureId()));
        }
        if (genericHandle != null
                && !inputActionQueue.checkRetainedSessionBoundary(
                        taskRunRegistry.genericInputSession(genericHandle),
                        "capture-before-frame")) {
            try {
                taskRunRegistry.completeGenericStep(
                        genericHandle, request.getSessionRef(), true);
            } catch (RuntimeException staleTerminalStep) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture pre-frame budget fence lost its exact cursor",
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
            return terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    retainedTerminalOutcomeCode(
                            taskRunRegistry.genericInputSession(genericHandle)
                                    .terminalSnapshot()),
                    "generic capture did not start because its retained boundary closed",
                    timing, emptyCapturePayload(request.getCaptureId()));
        }
        if (genericHandle != null
                && taskRunRegistry.genericInputSession(genericHandle)
                        .terminalSnapshot() != null) {
            try {
                taskRunRegistry.completeGenericStep(
                        genericHandle, request.getSessionRef(), true);
            } catch (RuntimeException staleTerminalStep) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture terminal snapshot lost its exact cursor",
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
            return terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    retainedTerminalOutcomeCode(
                            taskRunRegistry.genericInputSession(genericHandle)
                                    .terminalSnapshot()),
                    "generic capture did not start because its retained worker ended",
                    timing, emptyCapturePayload(request.getCaptureId()));
        }
        RemoteGameOutcomeEnvelope outcome = executeCaptureMechanical(
                command, request, genericHandle, registration, access, timing);
        if (genericHandle != null
                && outcome.getExecutionState() == RemoteExecutionState.OBSERVED
                && !inputActionQueue.checkRetainedSessionBoundary(
                        taskRunRegistry.genericInputSession(genericHandle),
                        "capture-after-frame")) {
            return terminal(
                    command, registration, RemoteExecutionState.UNKNOWN,
                    retainedTerminalOutcomeCode(
                            taskRunRegistry.genericInputSession(genericHandle)
                                    .terminalSnapshot()),
                    "generic capture frame was discarded because its retained boundary closed",
                    timing, emptyCapturePayload(request.getCaptureId()));
        }
        if (genericHandle != null && outcome.getExecutionState() != RemoteExecutionState.UNKNOWN) {
            try {
                if (taskRunRegistry.genericInputSession(genericHandle)
                        .terminalSnapshot() != null) {
                    throw new IllegalStateException(
                            "retained input owner ended during generic capture");
                }
                taskRunRegistry.completeGenericStep(
                        genericHandle, request.getSessionRef(), false);
            } catch (RuntimeException staleAfterCapture) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture completed but its post-execution fence changed",
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
        }
        return outcome;
    }

    private RemoteGameOutcomeEnvelope executeCaptureMechanical(
            RemoteGameCommand command,
            RemoteCaptureCommandPayload request,
            RemoteTaskRunRegistry.InFlightExclusiveHandle genericHandle,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) throws Exception {
        CaptureRectangle rectangle;
        try {
            rectangle = captureRectangle(request.getRegion(), access.binding());
        } catch (RuntimeException invalidRegion) {
            if (genericHandle == null) {
                throw invalidRegion;
            }
            return terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    "generic capture region rejected before frame: "
                            + safeMessage(invalidRegion),
                    timing, emptyCapturePayload(request.getCaptureId()));
        }
        // Bracket the single frame with the same live system fact source. scaleBefore is read before
        // any capture begins, so an unreadable/illegal value is a clean NOT_EXECUTED with no frame.
        Double scaleBefore = readSystemScaleRatioNow();
        if (!isValidSystemScaleRatio(scaleBefore)) {
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.FACT_UNAVAILABLE,
                    "system scale ratio was unavailable before capture",
                    timing,
                    emptyCapturePayload(request.getCaptureId()));
        }
        if (genericHandle != null) {
            try {
                taskRunRegistry.requireGenericExclusiveStepCurrent(
                        genericHandle, request.getSessionRef());
            } catch (RuntimeException staleBeforeCapture) {
                return terminal(
                        command, registration, RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic capture pre-execution fence changed",
                        timing, emptyCapturePayload(request.getCaptureId()));
            }
        }
        Optional<BoundWindowCaptureService.CaptureResult> captured = captureService.captureRegion(
                access.binding(),
                access.binding().getX(),
                access.binding().getY(),
                rectangle.x1(),
                rectangle.y1(),
                rectangle.x2(),
                rectangle.y2());
        if (captured.isEmpty()) {
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.CAPTURE_FAILED,
                    "bound HWND capture returned no image",
                    timing,
                    emptyCapturePayload(request.getCaptureId()));
        }

        // scaleAfter closes the bracket. The frame already exists, so instability cannot be reported
        // as NOT_EXECUTED: flush the image and return UNKNOWN so Cloud never treats it as a miss.
        Double scaleAfter = readSystemScaleRatioNow();
        if (!isValidSystemScaleRatio(scaleAfter)
                || Double.doubleToLongBits(scaleBefore) != Double.doubleToLongBits(scaleAfter)) {
            captured.get().image().flush();
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.UNKNOWN,
                    RemoteOutcomeCode.FACT_UNAVAILABLE,
                    "system scale ratio was unstable across capture",
                    timing,
                    emptyCapturePayload(request.getCaptureId()));
        }

        byte[] pngBytes;
        BufferedImage image = captured.get().image();
        int capturedWidth = image.getWidth();
        int capturedHeight = image.getHeight();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                return terminal(
                        command,
                        registration,
                        genericHandle == null
                                ? RemoteExecutionState.NOT_EXECUTED
                                : RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.CAPTURE_FAILED,
                        "PNG encoder is unavailable",
                        timing,
                        emptyCapturePayload(request.getCaptureId()));
            }
            pngBytes = output.toByteArray();
        } finally {
            image.flush();
        }

        if (timing.timedOut()) {
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.UNKNOWN,
                    RemoteOutcomeCode.TIMEOUT,
                    "capture completed after command timeout",
                    timing,
                    emptyCapturePayload(request.getCaptureId()));
        }
        BindingAccess observedAccess;
        try {
            observedAccess = requireBoundWindow(command, true);
            requireRegistration(command, observedAccess.runner(), true);
            if (!access.binding().hasSameGeometry(observedAccess.binding())) {
                throw new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.WINDOW_BINDING_CHANGED,
                        "window geometry changed during capture");
            }
        } catch (TerminalSignal postCaptureFenceFailure) {
            if (genericHandle == null) {
                throw postCaptureFenceFailure;
            }
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.UNKNOWN,
                    postCaptureFenceFailure.code,
                    "generic capture completed but its window fence changed: "
                            + postCaptureFenceFailure.getMessage(),
                    timing,
                    emptyCapturePayload(request.getCaptureId()));
        }
        RemoteCaptureOutcomePayload payload = RemoteCaptureOutcomePayload.builder()
                .captureId(request.getCaptureId())
                .imageBytes(pngBytes)
                .imageSha256(protocolDigests.sha256Hex(pngBytes))
                .width(capturedWidth)
                .height(capturedHeight)
                .captureProvider(RemoteCaptureProvider.valueOf(captured.get().provider().name()))
                .systemScaleRatio(scaleBefore)
                .observedWindow(observedWindow(observedAccess.context(), observedAccess.binding()))
                .build();
        return terminal(
                command,
                registration,
                RemoteExecutionState.OBSERVED,
                RemoteOutcomeCode.OK,
                "capture completed",
                timing,
                payload);
    }

    private RemoteGameOutcomeEnvelope executeWindowFact(
            RemoteGameCommand command,
            RemoteWindowFactCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        boolean localStopRequested = localStopRequested(access.runner());

        Object fact = switch (request.getFactKind()) {
            case BINDING -> RemoteBindingFact.builder()
                    .windowId(access.context().getWindowId())
                    .nativeHandle(access.binding().getNativeHandle())
                    .processId(access.binding().getProcessId())
                    .playerIdentityEpoch(access.context().getPlayerIdentityEpoch())
                    .title(access.binding().getTitle())
                    .className(access.binding().getClassName())
                    .build();
            case GEOMETRY -> {
                if (!access.binding().hasGeometry()) {
                    throw new TerminalSignal(
                            RemoteExecutionState.NOT_EXECUTED,
                            RemoteOutcomeCode.FACT_UNAVAILABLE,
                            "bound window geometry is unavailable");
                }
                yield RemoteGeometryFact.builder()
                        .x(access.binding().getX())
                        .y(access.binding().getY())
                        .width(access.binding().getWidth())
                        .height(access.binding().getHeight())
                        .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                        .build();
            }
            case FOCUS_STATE -> RemoteFocusFact.builder()
                    .state(focusState(access.binding()))
                    .build();
            case STOP_STATE -> RemoteStopFact.builder()
                    .taskRunId(registration.getTaskRunId())
                    .stopEpoch(registration.getStopEpoch())
                    .stopRequested(registration.getStatus() == RemoteTaskRunStatus.STOPPING
                            || registration.getStatus().isTerminal()
                            || localStopRequested)
                    .build();
            case LEFT_TOP_STATUS -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> leftTopStatusSwitchService.probeLeftTopStatusFact(
                            "remote-window-fact:left-top-status"));
            case AUTO_COMBAT_PANEL -> windowTaskContextHolder.callWith(
                    access.context(),
                    autoCombatPanelService::probeAutoCombatPanelFact);
            case COMMON_BOX -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toCommonBoxFact(
                            commonBoxLocalObservationMechanics.observe(access.binding()),
                            access.binding()));
            case TEAM_RETURN_BUTTON -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toTeamReturnButtonFact(
                            teamReturnButtonLocalObservationMechanics.observe(access.binding()),
                            access.binding()));
            case TEAM_RETURN_LEADER_SIGNAL -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toTeamReturnLeaderSignalFact(
                            teamReturnLeaderSignalLocalObservationMechanics.observe(access.binding()),
                            access.binding()));
            case TASK_TRACKER_PANEL_RECT -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toTaskTrackerPanelRectFact(
                            taskTrackerPanelRectLocalObservationMechanics.observe(access.binding())));
            case BATTLE_RADAR_AUTO_FLAG -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarSignalFact(
                            battleRadarLocalObservationMechanics.observeAutoFlag(access.binding())));
            case BATTLE_RADAR_SELECTION_SIGNAL -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarSignalFact(
                            battleRadarLocalObservationMechanics.observeSelectionSignal(access.binding())));
            case BATTLE_RADAR_TOP_SIGNAL -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarSignalFact(
                            battleRadarLocalObservationMechanics.observeTopSignal(access.binding())));
            case BATTLE_RADAR_MINIMAP_READABLE -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarMinimapFact(
                            battleRadarLocalObservationMechanics.observeMinimapReadable(access.binding())));
            case BATTLE_RADAR_AVATAR_BASELINE -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarAvatarFact(
                            battleRadarLocalObservationMechanics.observeAvatarBaseline(
                                    access.binding(),
                                    access.context().getWindowId(),
                                    access.context().getPlayerIdentityEpoch())));
            case BATTLE_RADAR_AVATAR_PROBE -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarAvatarFact(
                            battleRadarLocalObservationMechanics.observeAvatarProbe(
                                    access.binding(),
                                    access.context().getWindowId(),
                                    access.context().getPlayerIdentityEpoch())));
            case BATTLE_RADAR_AVATAR_REFRESH -> windowTaskContextHolder.callWith(
                    access.context(),
                    () -> toBattleRadarAvatarFact(
                            battleRadarLocalObservationMechanics.observeAvatarRefresh(
                                    access.binding(),
                                    access.context().getWindowId(),
                                    access.context().getPlayerIdentityEpoch())));
        };

        if (timing.timedOut()) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TIMEOUT,
                    "window fact read exceeded command timeout");
        }
        RemoteTaskRunRegistration observedRegistration = requireRegistration(command, null, false);
        BindingAccess observedAccess = requireBoundWindow(command, true);
        if (request.getFactKind() == RemoteWindowFactKind.STOP_STATE
                && (observedRegistration.getStatus() != registration.getStatus()
                || observedRegistration.getStopEpoch() != registration.getStopEpoch())) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TASK_RUN_MISMATCH,
                    "task run stop state changed during fact read");
        }
        RemoteWindowFactOutcomePayload payload = RemoteWindowFactOutcomePayload.builder()
                .factKind(request.getFactKind())
                .fact(payloadCodec.toPayloadTree(fact))
                .build();
        return terminal(
                command,
                observedRegistration,
                RemoteExecutionState.OBSERVED,
                RemoteOutcomeCode.OK,
                "window fact observed on hwnd=" + observedAccess.binding().getNativeHandle(),
                timing,
                payload);
    }

    /**
     * Projects one exact common-box local observation onto the closed {@link RemoteCommonBoxFact}.
     *
     * <p>The five mechanics statuses map one-to-one onto the fact state, so a mechanics/transport
     * failure ({@code CAPTURE_UNAVAILABLE}/{@code TEMPLATE_UNAVAILABLE}/{@code MECHANICS_FAILED}) is
     * never disguised as {@code NOT_MATCHED}. A {@code MATCHED} observation carries the window-client
     * point converted to screen-absolute pixels using the exact binding origin, plus score and
     * timestamp; every non-matched state carries only its state and the coordinate space.</p>
     *
     * @param result non-null closed observation from the local mechanics
     * @param binding exact native-window binding whose screen-absolute origin anchors the click point
     * @return closed screen-absolute common-box fact
     */
    private RemoteCommonBoxFact toCommonBoxFact(
            CommonBoxLocalObservationMechanics.ObservationResult result,
            WindowNativeBinding binding) {
        RemoteCommonBoxFact.State state = switch (result.status()) {
            case MATCHED -> RemoteCommonBoxFact.State.MATCHED;
            case NOT_MATCHED -> RemoteCommonBoxFact.State.NOT_MATCHED;
            case CAPTURE_UNAVAILABLE -> RemoteCommonBoxFact.State.CAPTURE_UNAVAILABLE;
            case TEMPLATE_UNAVAILABLE -> RemoteCommonBoxFact.State.TEMPLATE_UNAVAILABLE;
            case MECHANICS_FAILED -> RemoteCommonBoxFact.State.MECHANICS_FAILED;
        };
        if (state != RemoteCommonBoxFact.State.MATCHED) {
            return RemoteCommonBoxFact.builder()
                    .state(state)
                    .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                    .build();
        }
        return RemoteCommonBoxFact.builder()
                .state(RemoteCommonBoxFact.State.MATCHED)
                .clickX(binding.getX() + result.clientX())
                .clickY(binding.getY() + result.clientY())
                .matchScore(result.matchScore())
                .matchedAtEpochMs(result.matchedAtEpochMs())
                .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                .build();
    }

    /**
     * Projects one exact team-return-button local observation onto the closed
     * {@link RemoteTeamReturnButtonFact}.
     *
     * <p>The five mechanics states map one-to-one onto the fact state, so a mechanics/transport
     * failure ({@code CAPTURE_UNAVAILABLE}/{@code TEMPLATE_UNAVAILABLE}/{@code MECHANICS_FAILED}) is
     * never disguised as {@code ABSENT}. A {@code PRESENT} observation carries the window-client point
     * converted to screen-absolute pixels using the exact binding origin, plus the match score; every
     * other state carries only its state and the coordinate space.</p>
     *
     * @param result non-null closed observation from the local mechanics
     * @param binding exact native-window binding whose screen-absolute origin anchors the click point
     * @return closed screen-absolute team-return-button fact
     */
    private RemoteTeamReturnButtonFact toTeamReturnButtonFact(
            TeamReturnButtonLocalObservationMechanics.ObservationResult result,
            WindowNativeBinding binding) {
        RemoteTeamReturnButtonFact.State state = switch (result.state()) {
            case PRESENT -> RemoteTeamReturnButtonFact.State.PRESENT;
            case ABSENT -> RemoteTeamReturnButtonFact.State.ABSENT;
            case CAPTURE_UNAVAILABLE -> RemoteTeamReturnButtonFact.State.CAPTURE_UNAVAILABLE;
            case TEMPLATE_UNAVAILABLE -> RemoteTeamReturnButtonFact.State.TEMPLATE_UNAVAILABLE;
            case MECHANICS_FAILED -> RemoteTeamReturnButtonFact.State.MECHANICS_FAILED;
        };
        if (state != RemoteTeamReturnButtonFact.State.PRESENT) {
            return RemoteTeamReturnButtonFact.builder()
                    .state(state)
                    .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                    .build();
        }
        return RemoteTeamReturnButtonFact.builder()
                .state(RemoteTeamReturnButtonFact.State.PRESENT)
                .clickX(binding.getX() + result.clientX())
                .clickY(binding.getY() + result.clientY())
                .matchScore(result.matchScore())
                .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                .build();
    }

    /**
     * Projects one exact team-return leader-signal local observation onto the closed
     * {@link RemoteTeamReturnLeaderSignalFact}.
     *
     * <p>The five mechanics states map one-to-one onto the fact state, so a mechanics/transport
     * failure ({@code CAPTURE_UNAVAILABLE}/{@code TEMPLATE_UNAVAILABLE}/{@code MECHANICS_FAILED}) is
     * never disguised as {@code ABSENT}. A {@code PRESENT} observation carries the window-client point
     * converted to screen-absolute pixels using the exact binding origin, plus the match score; every
     * other state carries only its state and the coordinate space.</p>
     *
     * @param result non-null closed observation from the local mechanics
     * @param binding exact native-window binding whose screen-absolute origin anchors the signal point
     * @return closed screen-absolute team-return leader-signal fact
     */
    private RemoteTeamReturnLeaderSignalFact toTeamReturnLeaderSignalFact(
            TeamReturnLeaderSignalLocalObservationMechanics.ObservationResult result,
            WindowNativeBinding binding) {
        RemoteTeamReturnLeaderSignalFact.State state = switch (result.state()) {
            case PRESENT -> RemoteTeamReturnLeaderSignalFact.State.PRESENT;
            case ABSENT -> RemoteTeamReturnLeaderSignalFact.State.ABSENT;
            case CAPTURE_UNAVAILABLE -> RemoteTeamReturnLeaderSignalFact.State.CAPTURE_UNAVAILABLE;
            case TEMPLATE_UNAVAILABLE -> RemoteTeamReturnLeaderSignalFact.State.TEMPLATE_UNAVAILABLE;
            case MECHANICS_FAILED -> RemoteTeamReturnLeaderSignalFact.State.MECHANICS_FAILED;
        };
        if (state != RemoteTeamReturnLeaderSignalFact.State.PRESENT) {
            return RemoteTeamReturnLeaderSignalFact.builder()
                    .state(state)
                    .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                    .build();
        }
        return RemoteTeamReturnLeaderSignalFact.builder()
                .state(RemoteTeamReturnLeaderSignalFact.State.PRESENT)
                .signalX(binding.getX() + result.clientX())
                .signalY(binding.getY() + result.clientY())
                .matchScore(result.matchScore())
                .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                .build();
    }

    /**
     * Projects one exact task-tracker panel-rectangle local observation onto the closed
     * {@link RemoteTaskTrackerPanelRectFact}.
     *
     * <p>The six mechanics states map one-to-one onto the fact state, so a mechanics/transport failure
     * ({@code CAPTURE_UNAVAILABLE}/{@code TEMPLATE_UNAVAILABLE}/{@code MECHANICS_FAILED}) or a
     * {@code REPOSITION_REQUIRED} safety gate is never disguised as {@code ABSENT}. Unlike the click
     * facts, a {@code PRESENT} observation carries the anchor point and panel rectangle verbatim in
     * window-client pixels with no screen shift, tagged {@code WINDOW_CLIENT_PX}; every other state
     * carries only its state and the coordinate space.</p>
     *
     * @param result non-null closed observation from the local mechanics
     * @return closed window-client task-tracker panel-rectangle fact
     */
    private RemoteTaskTrackerPanelRectFact toTaskTrackerPanelRectFact(
            TaskTrackerPanelRectLocalObservationMechanics.ObservationResult result) {
        RemoteTaskTrackerPanelRectFact.State state = switch (result.state()) {
            case PRESENT -> RemoteTaskTrackerPanelRectFact.State.PRESENT;
            case ABSENT -> RemoteTaskTrackerPanelRectFact.State.ABSENT;
            case CAPTURE_UNAVAILABLE -> RemoteTaskTrackerPanelRectFact.State.CAPTURE_UNAVAILABLE;
            case TEMPLATE_UNAVAILABLE -> RemoteTaskTrackerPanelRectFact.State.TEMPLATE_UNAVAILABLE;
            case REPOSITION_REQUIRED -> RemoteTaskTrackerPanelRectFact.State.REPOSITION_REQUIRED;
            case MECHANICS_FAILED -> RemoteTaskTrackerPanelRectFact.State.MECHANICS_FAILED;
        };
        if (state != RemoteTaskTrackerPanelRectFact.State.PRESENT) {
            return RemoteTaskTrackerPanelRectFact.builder()
                    .state(state)
                    .coordinateSpace(RemoteCoordinateSpace.WINDOW_CLIENT_PX)
                    .build();
        }
        return RemoteTaskTrackerPanelRectFact.builder()
                .state(RemoteTaskTrackerPanelRectFact.State.PRESENT)
                .anchorClientX(result.anchorClientX())
                .anchorClientY(result.anchorClientY())
                .panelClientLeft(result.panelClientLeft())
                .panelClientTop(result.panelClientTop())
                .panelClientRight(result.panelClientRight())
                .panelClientBottom(result.panelClientBottom())
                .matchScore(result.matchScore())
                .coordinateSpace(RemoteCoordinateSpace.WINDOW_CLIENT_PX)
                .build();
    }

    /**
     * Projects one exact battle-radar signal observation (auto flag / selection / top icons) onto the
     * closed {@link RemoteBattleRadarSignalFact}. The four mechanics statuses map one-to-one, so a
     * capture/mechanics failure is never disguised as {@code NOT_VISIBLE}.
     *
     * @param result non-null closed observation from the local mechanics
     * @return closed battle-radar signal fact
     */
    private RemoteBattleRadarSignalFact toBattleRadarSignalFact(
            BattleRadarLocalObservationMechanics.SignalResult result) {
        RemoteBattleRadarSignalFact.State state = switch (result.status()) {
            case VISIBLE -> RemoteBattleRadarSignalFact.State.VISIBLE;
            case NOT_VISIBLE -> RemoteBattleRadarSignalFact.State.NOT_VISIBLE;
            case CAPTURE_UNAVAILABLE -> RemoteBattleRadarSignalFact.State.CAPTURE_UNAVAILABLE;
            case MECHANICS_FAILED -> RemoteBattleRadarSignalFact.State.MECHANICS_FAILED;
        };
        return RemoteBattleRadarSignalFact.builder().state(state).build();
    }

    /**
     * Projects one exact battle-radar minimap readability observation onto the closed
     * {@link RemoteBattleRadarMinimapFact}. A mechanics failure is never disguised as
     * {@code UNREADABLE}.
     *
     * @param result non-null closed observation from the local mechanics
     * @return closed battle-radar minimap fact
     */
    private RemoteBattleRadarMinimapFact toBattleRadarMinimapFact(
            BattleRadarLocalObservationMechanics.MinimapResult result) {
        RemoteBattleRadarMinimapFact.State state = switch (result.status()) {
            case READABLE -> RemoteBattleRadarMinimapFact.State.READABLE;
            case UNREADABLE -> RemoteBattleRadarMinimapFact.State.UNREADABLE;
            case MECHANICS_FAILED -> RemoteBattleRadarMinimapFact.State.MECHANICS_FAILED;
        };
        return RemoteBattleRadarMinimapFact.builder().state(state).build();
    }

    /**
     * Projects one exact 20x20 avatar baseline/probe/refresh observation onto the closed
     * {@link RemoteBattleRadarAvatarFact}. The six mechanics statuses map one-to-one, so a capture or
     * mechanics failure is never disguised as {@code UNCHANGED}. The optional hover-client point and
     * screen-absolute ROI rectangle are carried verbatim as a full diagnostic group when the probe
     * located a configured hover point; state-only results carry no coordinates.
     *
     * @param result non-null closed observation from the local mechanics
     * @return closed battle-radar avatar fact
     */
    private RemoteBattleRadarAvatarFact toBattleRadarAvatarFact(
            BattleRadarLocalObservationMechanics.AvatarResult result) {
        RemoteBattleRadarAvatarFact.State state = switch (result.status()) {
            case BASELINE_CAPTURED -> RemoteBattleRadarAvatarFact.State.BASELINE_CAPTURED;
            case UNCHANGED -> RemoteBattleRadarAvatarFact.State.UNCHANGED;
            case CHANGED -> RemoteBattleRadarAvatarFact.State.CHANGED;
            case UNAVAILABLE -> RemoteBattleRadarAvatarFact.State.UNAVAILABLE;
            case NOT_CONFIGURED -> RemoteBattleRadarAvatarFact.State.NOT_CONFIGURED;
            case MECHANICS_FAILED -> RemoteBattleRadarAvatarFact.State.MECHANICS_FAILED;
        };
        return RemoteBattleRadarAvatarFact.builder()
                .state(state)
                .hoverClientX(result.hoverClientX())
                .hoverClientY(result.hoverClientY())
                .roiScreenLeft(result.roiScreenLeft())
                .roiScreenTop(result.roiScreenTop())
                .roiScreenRight(result.roiScreenRight())
                .roiScreenBottom(result.roiScreenBottom())
                .build();
    }

    /**
     * Run one closed LOCAL_MACRO variant inside the single serialized input queue.
     *
     * <p>The macro carries no exclusive session, so it acquires the input worker's exclusive section
     * directly through {@code submitRemoteExclusiveAndWaitDetailed} under the same session-less safety
     * fences as an input bundle. Each {@link BagService} entry runs exactly once on the input worker
     * and never re-acquires the queue. Stop/pause remains queue-driven, so the local bag core receives
     * a null {@code TaskExecutionContext}.</p>
     *
     * <p>Only EXECUTED carries the macroKind-specific typed result. STOPPED, NOT_EXECUTED, and UNKNOWN
     * carry explicit null result fields and never disguise a local observation.</p>
     */
    private RemoteGameOutcomeEnvelope executeLocalMacro(
            RemoteGameCommand command,
            RemoteLocalMacroCommandPayload request,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        if (request instanceof RemoteNavigateInCurrentMapMacroCommandPayload navigateRequest) {
            return executeNavigateInCurrentMapMacro(command, navigateRequest, registration, access, timing);
        }
        if (request instanceof RemoteUiCleanMacroCommandPayload uiCleanRequest) {
            return executeUiCleanMacro(command, uiCleanRequest, registration, access, timing);
        }
        if (request instanceof RemoteDialogDetectionMacroCommandPayload dialogRequest) {
            return executeDialogDetectionMacro(command, dialogRequest, registration, access, timing);
        }
        if (request instanceof RemotePlayerStateFirstAidMacroCommandPayload playerStateRequest) {
            return executePlayerStateFirstAidMacro(command, playerStateRequest, registration, access, timing);
        }
        if (request instanceof RemoteDialogPreparedActionValidationMacroCommandPayload preparedActionRequest) {
            return executeDialogPreparedActionValidationMacro(
                    command, preparedActionRequest, registration, access, timing);
        }
        if (request instanceof RemoteDialogOptionOcrImageMacroCommandPayload ocrImageRequest) {
            return executeDialogOptionOcrImageMacro(command, ocrImageRequest, registration, access, timing);
        }
        if (request instanceof RemoteDialogOptionOcrWordsMacroCommandPayload ocrWordsRequest) {
            return executeDialogOptionOcrWordsMacro(command, ocrWordsRequest, registration, access, timing);
        }
        if (request instanceof RemoteDialogWhiteStoryTemplateMacroCommandPayload whiteStoryRequest) {
            return executeDialogWhiteStoryTemplateMacro(command, whiteStoryRequest, registration, access, timing);
        }
        TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                        clientSession,
                        command.getTaskRunId(),
                        command.getWindow().getWindowId())
                .orElseThrow(() -> new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "remote task run pause token is unavailable"));
        BagReturnItemMacroIntent returnItemIntent =
                request instanceof RemoteBagReturnItemMacroCommandPayload returnItemRequest
                        ? toBagReturnItemMacroIntent(returnItemRequest)
                        : null;
        Object[] callbackResult = new Object[1];
        String description = request instanceof RemoteBagReturnItemMacroCommandPayload returnItemRequest
                ? "local-macro:bag-return-item:" + returnItemRequest.getOperation()
                : "local-macro:bag-use-incense";

        InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(
                access.context(),
                () -> inputActionQueue.submitRemoteExclusiveAndWaitDetailed(
                        description,
                        () -> {
                            if (request instanceof RemoteBagReturnItemMacroCommandPayload) {
                                callbackResult[0] = bagService.runReturnItemMacroDirectForExclusive(
                                        returnItemIntent, null);
                            } else if (request instanceof RemoteBagUseIncenseMacroCommandPayload) {
                                callbackResult[0] = bagService.runUseIncenseMacroDirectForExclusive(null);
                            }
                            return true;
                        },
                        timing.deadlineNanos(),
                        pauseToken,
                        () -> remoteInputSafetyReason(command, access.runner(), null),
                        () -> workerAdmissionRevisionFence(command, access.runner(), null)));

        boolean callbackStarted = executionResult.getStartedStepIndex() >= 0;
        InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                ? InputActionSafetyReason.CLEAR
                : executionResult.getSafetyReason();
        RemoteExecutionState executionState;
        RemoteOutcomeCode outcomeCode;
        Object outcomePayload;
        String message;
        if (executionResult.isCompleted() && callbackResult[0] != null) {
            executionState = RemoteExecutionState.EXECUTED;
            outcomeCode = RemoteOutcomeCode.OK;
            if (request instanceof RemoteBagReturnItemMacroCommandPayload returnItemRequest) {
                outcomePayload = toBagReturnItemMacroResultPayload(
                        returnItemRequest, (BagReturnItemMacroResult) callbackResult[0]);
            } else {
                outcomePayload = toBagUseIncenseMacroResultPayload((Boolean) callbackResult[0]);
            }
            message = "local macro executed: " + request.getMacroKind();
        } else if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
            executionState = RemoteExecutionState.STOPPED;
            outcomeCode = RemoteOutcomeCode.STOP_REQUESTED;
            outcomePayload = emptyOutcomePayload(command);
            message = "local macro stopped: " + request.getMacroKind()
                    + ": " + executionResult.getReason();
        } else if (!callbackStarted) {
            executionState = RemoteExecutionState.NOT_EXECUTED;
            outcomeCode = outcomeCodeForUnstarted(executionResult, safetyReason);
            outcomePayload = emptyOutcomePayload(command);
            message = "local macro not executed: " + request.getMacroKind()
                    + ": " + executionResult.getReason();
        } else {
            executionState = RemoteExecutionState.UNKNOWN;
            outcomeCode = safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                    ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                    : safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH
                            ? RemoteOutcomeCode.TASK_RUN_MISMATCH
                            : RemoteOutcomeCode.INPUT_FAILED;
            outcomePayload = emptyOutcomePayload(command);
            message = "local macro outcome unknown: " + request.getMacroKind()
                    + ": " + executionResult.getReason();
        }
        return terminal(command, registration, executionState, outcomeCode, message, timing, outcomePayload);
    }

    /**
     * UI_CLEAN variant. The three self-queued cleaners ({@code CLEAN_UP_ALL},
     * {@code CLOSE_ALL_GENERIC_WINDOWS}, {@code CLEAN_LIGHTWEIGHT_INTERRUPTIONS}) already own their own
     * input queue, so they run <em>outside</em> the single exclusive input queue through the same
     * window-context safety gate. {@code CLOSE_MAP_SEARCH_INPUT_BY_X2} is one direct interaction and
     * keeps the committed exclusive deadline/pause/safety/runRevision fences, calling only
     * {@link UICleanerService#closeMapSearchInputByX2Direct(String)} inside the exclusive callback with
     * no nested queue.
     */
    private RemoteGameOutcomeEnvelope executeUiCleanMacro(
            RemoteGameCommand command,
            RemoteUiCleanMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        RemoteUiCleanMacroCommandPayload.Operation operation = request.getOperation();
        if (operation == RemoteUiCleanMacroCommandPayload.Operation.CLOSE_MAP_SEARCH_INPUT_BY_X2) {
            return executeUiCleanCloseMapSearchInputByX2(command, request, registration, access, timing);
        }
        RemoteUiCleanMacroResultPayload.State state;
        try {
            state = windowTaskContextHolder.callWith(access.context(), () -> switch (operation) {
                case CLEAN_UP_ALL -> {
                    uiCleanerService.cleanUpAll();
                    yield RemoteUiCleanMacroResultPayload.State.COMPLETED;
                }
                case CLOSE_ALL_GENERIC_WINDOWS -> uiCleanerService.closeAllGenericWindows()
                        ? RemoteUiCleanMacroResultPayload.State.CLOSED_ANY
                        : RemoteUiCleanMacroResultPayload.State.NOTHING_CLOSED;
                case CLEAN_LIGHTWEIGHT_INTERRUPTIONS ->
                        uiCleanerService.cleanLightweightInterruptions(request.getSource())
                                ? RemoteUiCleanMacroResultPayload.State.HANDLED
                                : RemoteUiCleanMacroResultPayload.State.NOT_HANDLED;
                case CLOSE_MAP_SEARCH_INPUT_BY_X2 ->
                        throw new IllegalStateException("CLOSE_MAP_SEARCH_INPUT_BY_X2 is handled exclusively");
            });
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "ui-clean stopped: " + operation + ": " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                "ui-clean executed: " + operation + " -> " + state,
                timing, toUiCleanMacroResultPayload(operation, state));
    }

    private RemoteGameOutcomeEnvelope executeUiCleanCloseMapSearchInputByX2(
            RemoteGameCommand command,
            RemoteUiCleanMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                        clientSession,
                        command.getTaskRunId(),
                        command.getWindow().getWindowId())
                .orElseThrow(() -> new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "remote task run pause token is unavailable"));
        boolean[] callbackResult = new boolean[1];
        boolean[] callbackDone = new boolean[1];
        String description = "local-macro:ui-clean:close-map-search-input-by-x2";
        InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(
                access.context(),
                () -> inputActionQueue.submitRemoteExclusiveAndWaitDetailed(
                        description,
                        () -> {
                            callbackResult[0] = uiCleanerService.closeMapSearchInputByX2Direct(request.getSource());
                            callbackDone[0] = true;
                            return true;
                        },
                        timing.deadlineNanos(),
                        pauseToken,
                        () -> remoteInputSafetyReason(command, access.runner(), null),
                        () -> workerAdmissionRevisionFence(command, access.runner(), null)));

        boolean callbackStarted = executionResult.getStartedStepIndex() >= 0;
        InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                ? InputActionSafetyReason.CLEAR
                : executionResult.getSafetyReason();
        if (executionResult.isCompleted() && callbackDone[0]) {
            RemoteUiCleanMacroResultPayload.State state = callbackResult[0]
                    ? RemoteUiCleanMacroResultPayload.State.CLOSED
                    : RemoteUiCleanMacroResultPayload.State.NOT_FOUND;
            return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                    "ui-clean executed: CLOSE_MAP_SEARCH_INPUT_BY_X2 -> " + state,
                    timing, toUiCleanMacroResultPayload(
                            RemoteUiCleanMacroCommandPayload.Operation.CLOSE_MAP_SEARCH_INPUT_BY_X2, state));
        }
        if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "ui-clean stopped: CLOSE_MAP_SEARCH_INPUT_BY_X2: " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        if (!callbackStarted) {
            return terminal(command, registration, RemoteExecutionState.NOT_EXECUTED,
                    outcomeCodeForUnstarted(executionResult, safetyReason),
                    "ui-clean not executed: CLOSE_MAP_SEARCH_INPUT_BY_X2: " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        RemoteOutcomeCode unknownCode = safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                : safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH
                        ? RemoteOutcomeCode.TASK_RUN_MISMATCH
                        : RemoteOutcomeCode.INPUT_FAILED;
        return terminal(command, registration, RemoteExecutionState.UNKNOWN, unknownCode,
                "ui-clean outcome unknown: CLOSE_MAP_SEARCH_INPUT_BY_X2: " + executionResult.getReason(),
                timing, emptyOutcomePayload(command));
    }

    private Map<String, Object> toUiCleanMacroResultPayload(
            RemoteUiCleanMacroCommandPayload.Operation operation,
            RemoteUiCleanMacroResultPayload.State state) {
        RemoteUiCleanMacroResultPayload result = RemoteUiCleanMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.UI_CLEAN)
                .operation(operation)
                .state(state)
                .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", result.getMacroKind());
        outcome.put("operation", result.getOperation());
        outcome.put("state", result.getState());
        outcome.put("cachePoint", null);
        return outcome;
    }

    /**
     * DIALOG_DETECTION variant. Mirrors the committed same-path no-focus dialog detection. When
     * {@code hidePlayerNames} is false the whole detection is a pure read (pre-wait + capture + classify)
     * and runs <em>outside</em> the single input queue through the same window-context safety gate. When
     * true it needs the committed Alt+4 hide, so it runs once inside the exclusive input callback (on the
     * input worker) with no nested queue, keeping the committed deadline/pause/safety/runRevision fences.
     * The typed terminal losslessly carries the mechanical state and, when captured, the classified dialog.
     */
    private RemoteGameOutcomeEnvelope executeDialogDetectionMacro(
            RemoteGameCommand command,
            RemoteDialogDetectionMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        WindowNativeBinding binding = access.binding();
        String source = request.getSource();
        long waitMs = request.getWaitBeforeCaptureMs();
        if (!request.isHidePlayerNames()) {
            DialogDetectionLocalMechanics.DialogDetectionResult result;
            try {
                result = windowTaskContextHolder.callWith(access.context(),
                        () -> dialogDetectionLocalMechanics.detectDialog(binding, false, waitMs, source));
            } catch (TaskStopRequestedException stop) {
                return terminal(command, registration, RemoteExecutionState.STOPPED,
                        RemoteOutcomeCode.STOP_REQUESTED,
                        "dialog-detection stopped: " + stop.getMessage(),
                        timing, emptyOutcomePayload(command));
            }
            return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                    "dialog-detection executed: hide=false -> " + result.state(),
                    timing, toDialogDetectionMacroResultPayload(result));
        }
        TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                        clientSession,
                        command.getTaskRunId(),
                        command.getWindow().getWindowId())
                .orElseThrow(() -> new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "remote task run pause token is unavailable"));
        Object[] callbackResult = new Object[1];
        String description = "local-macro:dialog-detection:hide-player-names";
        InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(
                access.context(),
                () -> inputActionQueue.submitRemoteExclusiveAndWaitDetailed(
                        description,
                        () -> {
                            callbackResult[0] = dialogDetectionLocalMechanics.detectDialog(
                                    binding, true, waitMs, source);
                            return true;
                        },
                        timing.deadlineNanos(),
                        pauseToken,
                        () -> remoteInputSafetyReason(command, access.runner(), null),
                        () -> workerAdmissionRevisionFence(command, access.runner(), null)));

        boolean callbackStarted = executionResult.getStartedStepIndex() >= 0;
        InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                ? InputActionSafetyReason.CLEAR
                : executionResult.getSafetyReason();
        if (executionResult.isCompleted() && callbackResult[0] != null) {
            DialogDetectionLocalMechanics.DialogDetectionResult result =
                    (DialogDetectionLocalMechanics.DialogDetectionResult) callbackResult[0];
            return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                    "dialog-detection executed: hide=true -> " + result.state(),
                    timing, toDialogDetectionMacroResultPayload(result));
        }
        if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "dialog-detection stopped: " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        if (!callbackStarted) {
            return terminal(command, registration, RemoteExecutionState.NOT_EXECUTED,
                    outcomeCodeForUnstarted(executionResult, safetyReason),
                    "dialog-detection not executed: " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        RemoteOutcomeCode unknownCode = safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                : safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH
                        ? RemoteOutcomeCode.TASK_RUN_MISMATCH
                        : RemoteOutcomeCode.INPUT_FAILED;
        return terminal(command, registration, RemoteExecutionState.UNKNOWN, unknownCode,
                "dialog-detection outcome unknown: " + executionResult.getReason(),
                timing, emptyOutcomePayload(command));
    }

    /**
     * DIALOG_OPTION_OCR_IMAGE variant. A pure same-frame OCR-image preparation read: it captures the exact
     * dialog rect once (or reuses a supplied frame) and washes the raw/green/yellow variants, sending no
     * input, so it runs outside the single input queue through the same window-context safety gate.
     */
    private RemoteGameOutcomeEnvelope executeDialogOptionOcrImageMacro(
            RemoteGameCommand command,
            RemoteDialogOptionOcrImageMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        WindowNativeBinding binding = access.binding();
        // P1-3: re-verify the supplied frame's SHA-256 BEFORE invoking the mechanics. A mismatch (or an
        // unavailable digest) is a closed INVALID_SUPPLIED_FRAME typed result and the mechanics is never
        // called with unbound content. The mechanics keeps its own defense-in-depth SHA check.
        byte[] suppliedFrame = request.getSuppliedFramePngBytes();
        if (suppliedFrame != null) {
            String recomputedSha256 = dialogOptionOcrImageSha256(suppliedFrame);
            if (recomputedSha256 == null || !recomputedSha256.equals(request.getSuppliedFrameSha256())) {
                return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                        "dialog-option-ocr-image invalid supplied frame: SHA-256 mismatch",
                        timing, toDialogOptionOcrImageStatusPayload(
                                RemoteDialogOptionOcrImageMacroResultPayload.Status.INVALID_SUPPLIED_FRAME,
                                "supplied-frame-sha-mismatch"));
            }
        }
        DialogOptionOcrImageLocalObservationMechanics.DialogOcrImageIntent intent =
                new DialogOptionOcrImageLocalObservationMechanics.DialogOcrImageIntent(
                        request.getSuppliedFramePngBytes(),
                        request.getSuppliedFrameSha256(),
                        request.getRectLeft(),
                        request.getRectTop(),
                        request.getRectRight(),
                        request.getRectBottom(),
                        request.getSource());
        DialogOptionOcrImageLocalObservationMechanics.DialogOptionOcrImageResult result;
        try {
            result = windowTaskContextHolder.callWith(access.context(),
                    () -> dialogOptionOcrImageLocalObservationMechanics.prepareOptionOcrImages(binding, intent));
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "dialog-option-ocr-image stopped: " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                "dialog-option-ocr-image executed: " + result.status(),
                timing, toDialogOptionOcrImageMacroResultPayload(result));
    }

    /**
     * DIALOG_WHITE_STORY_TEMPLATE variant. Reproduces the committed one-authoritative-frame white
     * story-template observation through the approved local mechanics: it re-verifies any supplied frame's
     * SHA-256 fail-closed, rebuilds the caller-owned {@link DialogDetection} (image + rect + type) and the
     * caller-order {@link WhiteTemplateSpec} list, then runs the mechanics inside the window-context safety
     * gate. It sends no input; the Cloud caller keeps every target/action/fallback/absent/miss decision.
     */
    private RemoteGameOutcomeEnvelope executeDialogWhiteStoryTemplateMacro(
            RemoteGameCommand command,
            RemoteDialogWhiteStoryTemplateMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        WindowNativeBinding binding = access.binding();
        // Directive #2: re-verify the supplied frame's SHA-256 BEFORE the mechanics; a mismatch (or an
        // unavailable digest) fails closed to the MECHANICS_FAILED terminal and the supplied frame is never
        // trusted. A fully-absent command drives the mechanics fresh-detection fallback under its binding gate.
        byte[] suppliedFrame = request.getSuppliedFramePngBytes();
        DialogDetection suppliedDetection = null;
        if (suppliedFrame != null) {
            String recomputedSha256 = dialogOptionOcrImageSha256(suppliedFrame);
            if (recomputedSha256 == null || !recomputedSha256.equals(request.getSuppliedFrameSha256())) {
                return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                        "dialog-white-story invalid supplied frame: SHA-256 mismatch",
                        timing, toDialogWhiteStoryTemplateStatusPayload(
                                RemoteDialogWhiteStoryTemplateMacroResultPayload.State.MECHANICS_FAILED));
            }
            BufferedImage suppliedImage;
            try {
                suppliedImage = ImageIO.read(new java.io.ByteArrayInputStream(suppliedFrame));
            } catch (java.io.IOException | RuntimeException e) {
                suppliedImage = null;
            }
            if (suppliedImage == null) {
                return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                        "dialog-white-story invalid supplied frame: not decodable PNG",
                        timing, toDialogWhiteStoryTemplateStatusPayload(
                                RemoteDialogWhiteStoryTemplateMacroResultPayload.State.MECHANICS_FAILED));
            }
            suppliedDetection = DialogDetection.builder()
                    .type(request.getSuppliedFrameType())
                    .dialogRect(new int[]{request.getSuppliedFrameLeft(), request.getSuppliedFrameTop(),
                            request.getSuppliedFrameRight(), request.getSuppliedFrameBottom()})
                    .image(suppliedImage)
                    .build();
        }
        List<WhiteTemplateSpec> specs = new ArrayList<>();
        if (request.getSpecs() != null) {
            for (RemoteDialogWhiteStoryTemplateMacroCommandPayload.WhiteTemplateSpecEntry entry
                    : request.getSpecs()) {
                specs.add(new WhiteTemplateSpec(entry.getName(), entry.getTemplatePath()));
            }
        }
        DialogDetection suppliedDetectionFinal = suppliedDetection;
        DialogWhiteStoryTemplateLocalObservationMechanics.WhiteStoryTemplateObservation result;
        try {
            result = windowTaskContextHolder.callWith(access.context(),
                    () -> dialogWhiteStoryTemplateLocalObservationMechanics.observeWhiteStoryTemplate(
                            binding, suppliedDetectionFinal, request.isAbsentAllowed(), specs, request.getSource()));
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "dialog-white-story stopped: " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                "dialog-white-story executed: " + result.state(),
                timing, toDialogWhiteStoryTemplateMacroResultPayload(result));
    }

    private Map<String, Object> toDialogWhiteStoryTemplateStatusPayload(
            RemoteDialogWhiteStoryTemplateMacroResultPayload.State state) {
        return toDialogWhiteStoryTemplateOutcomeMap(
                RemoteDialogWhiteStoryTemplateMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE)
                        .state(state)
                        .build());
    }

    private Map<String, Object> toDialogWhiteStoryTemplateMacroResultPayload(
            DialogWhiteStoryTemplateLocalObservationMechanics.WhiteStoryTemplateObservation result) {
        int[] rect = result.frameRect();
        RemoteDialogWhiteStoryTemplateMacroResultPayload payload =
                RemoteDialogWhiteStoryTemplateMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE)
                        .state(RemoteDialogWhiteStoryTemplateMacroResultPayload.State.valueOf(result.state().name()))
                        .matchedTemplateName(result.matchedTemplateName())
                        .matchedTemplatePath(result.matchedTemplatePath())
                        .relativeX(result.relativeX())
                        .relativeY(result.relativeY())
                        .absoluteX(result.absoluteX())
                        .absoluteY(result.absoluteY())
                        .frameLeft(rect == null ? null : rect[0])
                        .frameTop(rect == null ? null : rect[1])
                        .frameRight(rect == null ? null : rect[2])
                        .frameBottom(rect == null ? null : rect[3])
                        .framePngBytes(result.framePngBytes())
                        .frameSha256(result.frameSha256())
                        .frameWidth(result.frameWidth())
                        .frameHeight(result.frameHeight())
                        .build();
        return toDialogWhiteStoryTemplateOutcomeMap(payload);
    }

    private Map<String, Object> toDialogWhiteStoryTemplateOutcomeMap(
            RemoteDialogWhiteStoryTemplateMacroResultPayload payload) {
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("state", payload.getState());
        outcome.put("matchedTemplateName", payload.getMatchedTemplateName());
        outcome.put("matchedTemplatePath", payload.getMatchedTemplatePath());
        outcome.put("relativeX", payload.getRelativeX());
        outcome.put("relativeY", payload.getRelativeY());
        outcome.put("absoluteX", payload.getAbsoluteX());
        outcome.put("absoluteY", payload.getAbsoluteY());
        outcome.put("frameLeft", payload.getFrameLeft());
        outcome.put("frameTop", payload.getFrameTop());
        outcome.put("frameRight", payload.getFrameRight());
        outcome.put("frameBottom", payload.getFrameBottom());
        outcome.put("framePngBytes", payload.getFramePngBytes());
        outcome.put("frameSha256", payload.getFrameSha256());
        outcome.put("frameWidth", payload.getFrameWidth());
        outcome.put("frameHeight", payload.getFrameHeight());
        return outcome;
    }

    private static String dialogOptionOcrImageSha256(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
    }

    private Map<String, Object> toDialogOptionOcrImageStatusPayload(
            RemoteDialogOptionOcrImageMacroResultPayload.Status status, String reason) {
        RemoteDialogOptionOcrImageMacroResultPayload payload =
                RemoteDialogOptionOcrImageMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE)
                        .status(status)
                        .reason(reason)
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("status", payload.getStatus());
        outcome.put("rawPngBytes", payload.getRawPngBytes());
        outcome.put("rawSha256", payload.getRawSha256());
        outcome.put("greenPngBytes", payload.getGreenPngBytes());
        outcome.put("greenSha256", payload.getGreenSha256());
        outcome.put("yellowPngBytes", payload.getYellowPngBytes());
        outcome.put("yellowSha256", payload.getYellowSha256());
        outcome.put("imageWidth", payload.getImageWidth());
        outcome.put("imageHeight", payload.getImageHeight());
        outcome.put("scanLeft", payload.getScanLeft());
        outcome.put("scanTop", payload.getScanTop());
        outcome.put("scanRight", payload.getScanRight());
        outcome.put("scanBottom", payload.getScanBottom());
        outcome.put("reason", payload.getReason());
        return outcome;
    }

    /**
     * DIALOG_OPTION_OCR_WORDS variant. A pure single-variant OCR read: it validates the supplied variant
     * PNG and runs the local OCR provider exactly once, sending no input, so it runs outside the single
     * input queue through the same window-context safety gate. Color selection, alias/keyword matching,
     * merge and fallback remain in the Cloud caller.
     */
    private RemoteGameOutcomeEnvelope executeDialogOptionOcrWordsMacro(
            RemoteGameCommand command,
            RemoteDialogOptionOcrWordsMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        int[] screenRect = new int[]{
                request.getRectLeft(), request.getRectTop(), request.getRectRight(), request.getRectBottom()};
        DialogOptionOcrWordsLocalObservationMechanics.ColorVariant variant =
                DialogOptionOcrWordsLocalObservationMechanics.ColorVariant.valueOf(request.getVariant().name());
        DialogOptionOcrWordsLocalObservationMechanics.OptionOcrWordsObservation result;
        try {
            result = windowTaskContextHolder.callWith(access.context(),
                    () -> dialogOptionOcrWordsLocalObservationMechanics.observeOptionWords(
                            variant,
                            request.getVariantPngBytes(),
                            request.getVariantSha256(),
                            request.getImageWidth(),
                            request.getImageHeight(),
                            screenRect,
                            request.getSource()));
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "dialog-option-ocr-words stopped: " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                "dialog-option-ocr-words executed: " + result.status(),
                timing, toDialogOptionOcrWordsMacroResultPayload(result));
    }

    private Map<String, Object> toDialogOptionOcrImageMacroResultPayload(
            DialogOptionOcrImageLocalObservationMechanics.DialogOptionOcrImageResult result) {
        RemoteDialogOptionOcrImageMacroResultPayload payload =
                RemoteDialogOptionOcrImageMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE)
                        .status(RemoteDialogOptionOcrImageMacroResultPayload.Status.valueOf(result.status().name()))
                        .rawPngBytes(result.rawPngBytes())
                        .rawSha256(result.rawSha256())
                        .greenPngBytes(result.greenPngBytes())
                        .greenSha256(result.greenSha256())
                        .yellowPngBytes(result.yellowPngBytes())
                        .yellowSha256(result.yellowSha256())
                        .imageWidth(result.imageWidth())
                        .imageHeight(result.imageHeight())
                        .scanLeft(result.scanLeft())
                        .scanTop(result.scanTop())
                        .scanRight(result.scanRight())
                        .scanBottom(result.scanBottom())
                        .reason(result.reason())
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("status", payload.getStatus());
        outcome.put("rawPngBytes", payload.getRawPngBytes());
        outcome.put("rawSha256", payload.getRawSha256());
        outcome.put("greenPngBytes", payload.getGreenPngBytes());
        outcome.put("greenSha256", payload.getGreenSha256());
        outcome.put("yellowPngBytes", payload.getYellowPngBytes());
        outcome.put("yellowSha256", payload.getYellowSha256());
        outcome.put("imageWidth", payload.getImageWidth());
        outcome.put("imageHeight", payload.getImageHeight());
        outcome.put("scanLeft", payload.getScanLeft());
        outcome.put("scanTop", payload.getScanTop());
        outcome.put("scanRight", payload.getScanRight());
        outcome.put("scanBottom", payload.getScanBottom());
        outcome.put("reason", payload.getReason());
        return outcome;
    }

    private Map<String, Object> toDialogOptionOcrWordsMacroResultPayload(
            DialogOptionOcrWordsLocalObservationMechanics.OptionOcrWordsObservation result) {
        List<RemoteDialogOptionOcrWordsMacroResultPayload.RemoteWordBox> boxes = null;
        if (result.status() == DialogOptionOcrWordsLocalObservationMechanics.Status.WORDS) {
            boxes = new ArrayList<>(result.wordBoxes().size());
            for (DialogOptionOcrWordsLocalObservationMechanics.WordBox box : result.wordBoxes()) {
                boxes.add(RemoteDialogOptionOcrWordsMacroResultPayload.RemoteWordBox.builder()
                        .text(box.text())
                        .x(box.x())
                        .y(box.y())
                        .left(box.left())
                        .top(box.top())
                        .width(box.width())
                        .height(box.height())
                        .score(box.score())
                        .build());
            }
        }
        RemoteDialogOptionOcrWordsMacroResultPayload payload =
                RemoteDialogOptionOcrWordsMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_OPTION_OCR_WORDS)
                        .status(RemoteDialogOptionOcrWordsMacroResultPayload.Status.valueOf(result.status().name()))
                        .wordBoxes(boxes)
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("status", payload.getStatus());
        outcome.put("wordBoxes", payload.getWordBoxes());
        return outcome;
    }

    private Map<String, Object> toDialogDetectionMacroResultPayload(
            DialogDetectionLocalMechanics.DialogDetectionResult result) {
        ImagePreprocessor.TextLinePatternStats stats = result.storyTextLineStats();
        RemoteDialogDetectionMacroResultPayload payload = RemoteDialogDetectionMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.DIALOG_DETECTION)
                .state(RemoteDialogDetectionMacroResultPayload.State.valueOf(result.state().name()))
                .dialogType(result.dialogType())
                .dialogLeft(result.dialogLeft())
                .dialogTop(result.dialogTop())
                .dialogRight(result.dialogRight())
                .dialogBottom(result.dialogBottom())
                .framePngBytes(result.framePngBytes())
                .frameSha256(result.frameSha256())
                .frameWidth(result.frameWidth())
                .frameHeight(result.frameHeight())
                .maskStddev(result.maskStddev())
                .optionGreenCount(result.optionGreenCount())
                .storyThinWhiteCount(result.storyThinWhiteCount())
                .storyGreenCount(result.storyGreenCount())
                .storyTextMatched(stats == null ? null : stats.matched())
                .storyQualifyingRows(stats == null ? null : stats.qualifyingRows())
                .storyMaxWhitePixelsInRow(stats == null ? null : stats.maxWhitePixelsInRow())
                .storyMaxClustersInRow(stats == null ? null : stats.maxClustersInRow())
                .storyMaxSpanInRow(stats == null ? null : stats.maxSpanInRow())
                .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("state", payload.getState());
        outcome.put("dialogType", payload.getDialogType());
        outcome.put("dialogLeft", payload.getDialogLeft());
        outcome.put("dialogTop", payload.getDialogTop());
        outcome.put("dialogRight", payload.getDialogRight());
        outcome.put("dialogBottom", payload.getDialogBottom());
        outcome.put("framePngBytes", payload.getFramePngBytes());
        outcome.put("frameSha256", payload.getFrameSha256());
        outcome.put("frameWidth", payload.getFrameWidth());
        outcome.put("frameHeight", payload.getFrameHeight());
        outcome.put("maskStddev", payload.getMaskStddev());
        outcome.put("optionGreenCount", payload.getOptionGreenCount());
        outcome.put("storyThinWhiteCount", payload.getStoryThinWhiteCount());
        outcome.put("storyGreenCount", payload.getStoryGreenCount());
        outcome.put("storyTextMatched", payload.getStoryTextMatched());
        outcome.put("storyQualifyingRows", payload.getStoryQualifyingRows());
        outcome.put("storyMaxWhitePixelsInRow", payload.getStoryMaxWhitePixelsInRow());
        outcome.put("storyMaxClustersInRow", payload.getStoryMaxClustersInRow());
        outcome.put("storyMaxSpanInRow", payload.getStoryMaxSpanInRow());
        return outcome;
    }

    /**
     * PLAYER_STATE_FIRST_AID variant. Exactly three closed operations. PROBE_SUPPLY_NO_FOCUS is a pure
     * no-input bars read and runs <em>outside</em> the single input queue through the same window-context
     * safety gate. HEAL_ALL and EXECUTE_CACHED_PLAN send real right-clicks, so each runs once inside one
     * exclusive input-worker callback with no nested queue, keeping the committed
     * deadline/pause/safety/runRevision fences. The typed terminal losslessly mirrors the mechanics.
     */
    private RemoteGameOutcomeEnvelope executePlayerStateFirstAidMacro(
            RemoteGameCommand command,
            RemotePlayerStateFirstAidMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        WindowNativeBinding binding = access.binding();
        RemotePlayerStateFirstAidMacroCommandPayload.Operation operation = request.getOperation();
        if (operation == RemotePlayerStateFirstAidMacroCommandPayload.Operation.PROBE_SUPPLY_NO_FOCUS) {
            PlayerStateFirstAidLocalMacroMechanics.FirstAidIntent intent = toFirstAidIntent(request);
            PlayerStateFirstAidLocalMacroMechanics.NoFocusProbeResult result;
            try {
                result = windowTaskContextHolder.callWith(access.context(),
                        () -> playerStateFirstAidLocalMacroMechanics.probeSupplyNoFocus(binding, null, intent));
            } catch (TaskStopRequestedException stop) {
                return terminal(command, registration, RemoteExecutionState.STOPPED,
                        RemoteOutcomeCode.STOP_REQUESTED,
                        "player-state first-aid stopped: PROBE_SUPPLY_NO_FOCUS: " + stop.getMessage(),
                        timing, emptyOutcomePayload(command));
            }
            return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                    "player-state first-aid executed: PROBE_SUPPLY_NO_FOCUS -> " + result.snapshotStatus(),
                    timing, toProbeResultPayload(result));
        }
        TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                        clientSession, command.getTaskRunId(), command.getWindow().getWindowId())
                .orElseThrow(() -> new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED, RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "remote task run pause token is unavailable"));
        Object[] callbackResult = new Object[1];
        String description = "local-macro:player-state-first-aid:" + operation;
        InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(
                access.context(),
                () -> inputActionQueue.submitRemoteExclusiveAndWaitDetailed(
                        description,
                        () -> {
                            if (operation
                                    == RemotePlayerStateFirstAidMacroCommandPayload.Operation.HEAL_ALL) {
                                callbackResult[0] = playerStateFirstAidLocalMacroMechanics.healAllDirect(
                                        binding, null, toFirstAidIntent(request));
                            } else {
                                callbackResult[0] = playerStateFirstAidLocalMacroMechanics
                                        .executeCachedFirstAidPlanDirect(binding, toCachedPlan(request));
                            }
                            return true;
                        },
                        timing.deadlineNanos(), pauseToken,
                        () -> remoteInputSafetyReason(command, access.runner(), null),
                        () -> workerAdmissionRevisionFence(command, access.runner(), null)));

        boolean callbackStarted = executionResult.getStartedStepIndex() >= 0;
        InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                ? InputActionSafetyReason.CLEAR : executionResult.getSafetyReason();
        if (executionResult.isCompleted() && callbackResult[0] != null) {
            Map<String, Object> payload =
                    operation == RemotePlayerStateFirstAidMacroCommandPayload.Operation.HEAL_ALL
                            ? toHealResultPayload(
                                    (PlayerStateFirstAidLocalMacroMechanics.HealAllResult) callbackResult[0])
                            : toCachedResultPayload(
                                    (PlayerStateFirstAidLocalMacroMechanics.CachedPlanStatus) callbackResult[0]);
            return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                    "player-state first-aid executed: " + operation, timing, payload);
        }
        if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "player-state first-aid stopped: " + operation + ": " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        if (!callbackStarted) {
            return terminal(command, registration, RemoteExecutionState.NOT_EXECUTED,
                    outcomeCodeForUnstarted(executionResult, safetyReason),
                    "player-state first-aid not executed: " + operation + ": " + executionResult.getReason(),
                    timing, emptyOutcomePayload(command));
        }
        RemoteOutcomeCode unknownCode = safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                : safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH
                        ? RemoteOutcomeCode.TASK_RUN_MISMATCH : RemoteOutcomeCode.INPUT_FAILED;
        return terminal(command, registration, RemoteExecutionState.UNKNOWN, unknownCode,
                "player-state first-aid outcome unknown: " + operation + ": " + executionResult.getReason(),
                timing, emptyOutcomePayload(command));
    }

    private PlayerStateFirstAidLocalMacroMechanics.FirstAidIntent toFirstAidIntent(
            RemotePlayerStateFirstAidMacroCommandPayload request) {
        return new PlayerStateFirstAidLocalMacroMechanics.FirstAidIntent(
                toTargetToggle(request.getPlayerHp()), toTargetToggle(request.getPlayerMp()),
                toTargetToggle(request.getPetHp()), toTargetToggle(request.getPetMp()));
    }

    private PlayerStateFirstAidLocalMacroMechanics.TargetToggle toTargetToggle(
            RemotePlayerStateFirstAidMacroCommandPayload.RemoteFirstAidToggle toggle) {
        return new PlayerStateFirstAidLocalMacroMechanics.TargetToggle(
                toggle.isEnabled(), toggle.getThreshold());
    }

    private PlayerStateFirstAidLocalMacroMechanics.CachedFirstAidPlan toCachedPlan(
            RemotePlayerStateFirstAidMacroCommandPayload request) {
        List<PlayerStateFirstAidLocalMacroMechanics.CachedFirstAidTarget> targets = new ArrayList<>();
        for (RemotePlayerStateFirstAidMacroCommandPayload.RemoteCachedFirstAidTarget target
                : request.getTargets()) {
            targets.add(new PlayerStateFirstAidLocalMacroMechanics.CachedFirstAidTarget(
                    target.getName(), target.getRelX(), target.getRelY(), target.getThreshold()));
        }
        return new PlayerStateFirstAidLocalMacroMechanics.CachedFirstAidPlan(
                request.getPlanBaseX(), request.getPlanBaseY(), targets);
    }

    private Map<String, Object> toProbeResultPayload(
            PlayerStateFirstAidLocalMacroMechanics.NoFocusProbeResult result) {
        List<RemotePlayerStateFirstAidMacroResultPayload.RemoteProbeObservation> observations =
                new ArrayList<>();
        for (PlayerStateFirstAidLocalMacroMechanics.ProbeObservation obs : result.observations()) {
            observations.add(RemotePlayerStateFirstAidMacroResultPayload.RemoteProbeObservation.builder()
                    .name(obs.name()).status(obs.status())
                    .sampleRelX(obs.sampleRelX()).sampleRelY(obs.sampleRelY()).build());
        }
        return playerStateFirstAidResultNode(RemotePlayerStateFirstAidMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID)
                .operation(RemotePlayerStateFirstAidMacroCommandPayload.Operation.PROBE_SUPPLY_NO_FOCUS)
                .probeSnapshotStatus(result.snapshotStatus())
                .probeObservations(observations)
                .observedBaseX(result.observedBaseX())
                .observedBaseY(result.observedBaseY())
                .build());
    }

    private Map<String, Object> toHealResultPayload(
            PlayerStateFirstAidLocalMacroMechanics.HealAllResult result) {
        List<RemotePlayerStateFirstAidMacroResultPayload.RemoteHealOutcome> outcomes = new ArrayList<>();
        for (PlayerStateFirstAidLocalMacroMechanics.HealOutcome out : result.outcomes()) {
            outcomes.add(RemotePlayerStateFirstAidMacroResultPayload.RemoteHealOutcome.builder()
                    .name(out.name()).status(out.status())
                    .sampleRelX(out.sampleRelX()).sampleRelY(out.sampleRelY())
                    .clickAbsX(out.clickAbsX()).clickAbsY(out.clickAbsY()).build());
        }
        return playerStateFirstAidResultNode(RemotePlayerStateFirstAidMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID)
                .operation(RemotePlayerStateFirstAidMacroCommandPayload.Operation.HEAL_ALL)
                .healSnapshotStatus(result.snapshotStatus())
                .healOutcomes(outcomes)
                .build());
    }

    private Map<String, Object> toCachedResultPayload(
            PlayerStateFirstAidLocalMacroMechanics.CachedPlanStatus status) {
        return playerStateFirstAidResultNode(RemotePlayerStateFirstAidMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID)
                .operation(RemotePlayerStateFirstAidMacroCommandPayload.Operation.EXECUTE_CACHED_PLAN)
                .cachedPlanStatus(status)
                .build());
    }

    private Map<String, Object> playerStateFirstAidResultNode(
            RemotePlayerStateFirstAidMacroResultPayload payload) {
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("operation", payload.getOperation());
        outcome.put("probeSnapshotStatus", payload.getProbeSnapshotStatus());
        outcome.put("probeObservations", payload.getProbeObservations());
        outcome.put("healSnapshotStatus", payload.getHealSnapshotStatus());
        outcome.put("healOutcomes", payload.getHealOutcomes());
        outcome.put("cachedPlanStatus", payload.getCachedPlanStatus());
        outcome.put("observedBaseX", payload.getObservedBaseX());
        outcome.put("observedBaseY", payload.getObservedBaseY());
        return outcome;
    }

    /**
     * DIALOG_PREPARED_ACTION_VALIDATION variant. The whole validation is a pure no-input read (fresh
     * exact-HWND geometry, one capture, wash/fingerprint/distance), so it runs <em>outside</em> the single
     * input queue through the same window-context safety gate. The typed terminal losslessly carries the
     * closed validation state and, when measured, the current fingerprint/distance/maxDistance.
     */
    private RemoteGameOutcomeEnvelope executeDialogPreparedActionValidationMacro(
            RemoteGameCommand command,
            RemoteDialogPreparedActionValidationMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        WindowNativeBinding binding = access.binding();
        DialogPreparedActionValidationLocalMechanics.PreparedActionValidationResult result;
        try {
            result = windowTaskContextHolder.callWith(access.context(),
                    () -> dialogPreparedActionValidationLocalMechanics.validate(
                            binding, request.getValidationLeft(), request.getValidationTop(),
                            request.getValidationRight(), request.getValidationBottom(),
                            request.getWashMode(), request.getExpectedFingerprint(), request.getMaxDistance()));
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "dialog prepared-action validation stopped: " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                "dialog prepared-action validation executed -> " + result.state(),
                timing, toDialogPreparedActionValidationResultPayload(result));
    }

    private Map<String, Object> toDialogPreparedActionValidationResultPayload(
            DialogPreparedActionValidationLocalMechanics.PreparedActionValidationResult result) {
        RemoteDialogPreparedActionValidationMacroResultPayload payload =
                RemoteDialogPreparedActionValidationMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.DIALOG_PREPARED_ACTION_VALIDATION)
                        .state(result.state())
                        .currentFingerprint(result.currentFingerprint())
                        .distance(result.distance())
                        .maxDistance(result.maxDistance())
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("state", payload.getState());
        outcome.put("currentFingerprint", payload.getCurrentFingerprint());
        outcome.put("distance", payload.getDistance());
        outcome.put("maxDistance", payload.getMaxDistance());
        return outcome;
    }

    /**
     * NAVIGATE_IN_CURRENT_MAP variant: the closed macro is one logical operation, not a 60-second
     * hold of the input worker. It keeps the exact window-context safety gate and, <em>outside</em> the
     * single input queue, synchronously drives the existing committed local
     * {@link NavigationService#navigateInCurrentMap(NavigationRequest)}, which keeps using its own
     * input queue and pathing watcher. The full committed {@code NavigationResultStatus} and message
     * are preserved on the typed terminal.
     */
    private RemoteGameOutcomeEnvelope executeNavigateInCurrentMapMacro(
            RemoteGameCommand command,
            RemoteNavigateInCurrentMapMacroCommandPayload request,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        NavigationRequest restored = toNavigationRequest(request);
        NavigationResult result;
        try {
            result = windowTaskContextHolder.callWith(
                    access.context(),
                    () -> navigationService.navigateInCurrentMap(restored));
        } catch (TaskStopRequestedException stop) {
            return terminal(command, registration, RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "navigate-in-current-map stopped: " + stop.getMessage(),
                    timing, emptyOutcomePayload(command));
        }
        if (result == null) {
            return terminal(command, registration, RemoteExecutionState.UNKNOWN,
                    RemoteOutcomeCode.INPUT_FAILED,
                    "navigate-in-current-map returned no result", timing, emptyOutcomePayload(command));
        }
        return terminal(command, registration, RemoteExecutionState.EXECUTED, RemoteOutcomeCode.OK,
                result.getMessage(), timing, toNavigateInCurrentMapMacroResultPayload(result));
    }

    private NavigationRequest toNavigationRequest(RemoteNavigateInCurrentMapMacroCommandPayload request) {
        return NavigationRequest.builder()
                .targetMapName(request.getTargetMapName())
                .targetX(request.getTargetX())
                .targetY(request.getTargetY())
                .targetName(request.getTargetName())
                .randomizeMiniMapClickPoint(request.isRandomizeMiniMapClickPoint())
                .miniMapClickRandomRadiusPx(request.getMiniMapClickRandomRadiusPx())
                .keepTurnOnCurrentMapPathing(request.isKeepTurnOnCurrentMapPathing())
                .arrivalTolerance(request.getArrivalTolerance())
                .source(request.getSource())
                .freshCurrentMapName(request.getFreshCurrentMapName())
                .freshCurrentX(request.getFreshCurrentX())
                .freshCurrentY(request.getFreshCurrentY())
                .freshCurrentLocationAtMs(request.getFreshCurrentLocationAtMs())
                .freshCurrentLocationPhaseBound(request.isFreshCurrentLocationPhaseBound())
                .build();
    }

    private Map<String, Object> toNavigateInCurrentMapMacroResultPayload(NavigationResult result) {
        RemoteNavigateInCurrentMapMacroResultPayload.State state = switch (result.getStatus()) {
            case ARRIVED -> RemoteNavigateInCurrentMapMacroResultPayload.State.ARRIVED;
            case PATHING_STARTED -> RemoteNavigateInCurrentMapMacroResultPayload.State.PATHING_STARTED;
            case SUCCESS -> RemoteNavigateInCurrentMapMacroResultPayload.State.SUCCESS;
            case FAILED -> RemoteNavigateInCurrentMapMacroResultPayload.State.FAILED;
            case STOPPED -> RemoteNavigateInCurrentMapMacroResultPayload.State.STOPPED;
            case INTERRUPTED -> RemoteNavigateInCurrentMapMacroResultPayload.State.INTERRUPTED;
            case DIALOG_PREPARING -> RemoteNavigateInCurrentMapMacroResultPayload.State.DIALOG_PREPARING;
            case MAP_NOT_REACHED -> RemoteNavigateInCurrentMapMacroResultPayload.State.MAP_NOT_REACHED;
            case POINT_NOT_REACHED -> RemoteNavigateInCurrentMapMacroResultPayload.State.POINT_NOT_REACHED;
            case DIALOG_OPENED -> RemoteNavigateInCurrentMapMacroResultPayload.State.DIALOG_OPENED;
        };
        RemoteNavigateInCurrentMapMacroResultPayload payload =
                RemoteNavigateInCurrentMapMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.NAVIGATE_IN_CURRENT_MAP)
                        .state(state)
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", payload.getMacroKind());
        outcome.put("operation", null);
        outcome.put("state", payload.getState());
        outcome.put("cachePoint", null);
        return outcome;
    }

    private BagReturnItemMacroIntent toBagReturnItemMacroIntent(
            RemoteBagReturnItemMacroCommandPayload request) {
        return switch (request.getOperation()) {
            case PRESCAN_MAIN_BAG_TASK_PAGE -> BagReturnItemMacroIntent.prescanTaskPage(
                    request.getTemplatePath(), request.getSource());
            case PRESCAN_MAIN_BAG_FROM_BACK -> BagReturnItemMacroIntent.prescanFromBack(
                    request.getTemplatePath(), request.getMaxBackPage(), request.getSource());
            case USE_CACHED_MAIN_BAG_RETURN_ITEM -> BagReturnItemMacroIntent.useCachedReturnItem(
                    toReturnItemCachePoint(request.getCachedPoint()), request.getSource());
        };
    }

    private ReturnItemCachePoint toReturnItemCachePoint(
            RemoteBagReturnItemMacroCommandPayload.CachePoint wire) {
        return ReturnItemCachePoint.builder()
                .templatePath(wire.getTemplatePath())
                .clickX(wire.getClickX())
                .clickY(wire.getClickY())
                .learnedAtMs(wire.getLearnedAtMs())
                .source(wire.getSource())
                .build();
    }

    private RemoteBagReturnItemMacroResultPayload toBagReturnItemMacroResultPayload(
            RemoteBagReturnItemMacroCommandPayload request,
            BagReturnItemMacroResult result) {
        RemoteBagReturnItemMacroResultPayload.State state = switch (result.getStatus()) {
            case FOUND -> RemoteBagReturnItemMacroResultPayload.State.FOUND;
            case NOT_FOUND -> RemoteBagReturnItemMacroResultPayload.State.NOT_FOUND;
            case USED -> RemoteBagReturnItemMacroResultPayload.State.USED;
            case NOT_USED -> RemoteBagReturnItemMacroResultPayload.State.NOT_USED;
        };
        if (result.getStatus() == BagReturnItemMacroResult.Status.FOUND) {
            ReturnItemCachePoint point = result.getCachePoint();
            return RemoteBagReturnItemMacroResultPayload.builder()
                    .macroKind(request.getMacroKind())
                    .operation(request.getOperation())
                    .state(state)
                    .cachePoint(RemoteBagReturnItemMacroCommandPayload.CachePoint.builder()
                            .templatePath(point.getTemplatePath())
                            .clickX(point.getClickX())
                            .clickY(point.getClickY())
                            .learnedAtMs(point.getLearnedAtMs())
                            .source(point.getSource())
                            .build())
                    .build();
        }
        return RemoteBagReturnItemMacroResultPayload.builder()
                .macroKind(request.getMacroKind())
                .operation(request.getOperation())
                .state(state)
                .build();
    }

    private Map<String, Object> toBagUseIncenseMacroResultPayload(boolean used) {
        RemoteBagUseIncenseMacroResultPayload result =
                RemoteBagUseIncenseMacroResultPayload.builder()
                        .macroKind(RemoteLocalMacroKind.BAG_USE_INCENSE)
                        .state(used
                                ? RemoteBagUseIncenseMacroResultPayload.State.USED
                                : RemoteBagUseIncenseMacroResultPayload.State.NOT_FOUND)
                        .build();
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("macroKind", result.getMacroKind());
        outcome.put("operation", null);
        outcome.put("state", result.getState());
        outcome.put("cachePoint", null);
        return outcome;
    }

    private RemoteGameOutcomeEnvelope executeInputBundle(
            RemoteGameCommand command,
            RemoteInputBundleCommandPayload request,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        RemoteTaskRunRegistry.InFlightExclusiveHandle genericHandle = null;
        if (request.getSessionRef() != null) {
            try {
                genericHandle = taskRunRegistry.bindGenericExclusiveStep(
                        admissionSnapshot, command, request.getSessionRef());
            } catch (IllegalStateException staleSession) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic input session fence rejected before execution: "
                                + staleSession.getMessage(),
                        timing, emptyInputPayload(request.getActions().size()));
            }
        }
        if (genericHandle != null) {
            InputActionExecutionResult terminalSnapshot =
                    taskRunRegistry.genericInputSession(genericHandle).terminalSnapshot();
            if (terminalSnapshot != null) {
                try {
                    taskRunRegistry.completeGenericStep(
                            genericHandle, request.getSessionRef(), true);
                } catch (RuntimeException staleTerminalStep) {
                    return terminal(
                            command, registration, RemoteExecutionState.UNKNOWN,
                            RemoteOutcomeCode.TASK_RUN_MISMATCH,
                            "generic input terminal snapshot lost its exact cursor",
                            timing, emptyInputPayload(request.getActions().size()));
                }
                return terminal(
                        command, registration, RemoteExecutionState.NOT_EXECUTED,
                        retainedTerminalOutcomeCode(terminalSnapshot),
                        "generic input did not start because its retained worker ended: "
                                + terminalSnapshot.getReason(),
                        timing, emptyInputPayload(request.getActions().size()));
            }
        }
        RemoteGameOutcomeEnvelope outcome;
        try {
            outcome = executeInputBundleMechanical(
                    command, request, genericHandle, registration, access, timing);
        } catch (TerminalSignal terminalBeforeSubmission) {
            if (genericHandle == null) {
                throw terminalBeforeSubmission;
            }
            outcome = terminal(
                    command, registration, terminalBeforeSubmission.executionState,
                    terminalBeforeSubmission.code, terminalBeforeSubmission.getMessage(), timing,
                    emptyInputPayload(request.getActions().size()));
        } catch (IllegalArgumentException invalidBeforeSubmission) {
            if (genericHandle == null) {
                throw invalidBeforeSubmission;
            }
            outcome = terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    "generic input rejected before retained step submission: "
                            + safeMessage(invalidBeforeSubmission),
                    timing, emptyInputPayload(request.getActions().size()));
        }
        if (genericHandle != null && outcome.getExecutionState() != RemoteExecutionState.UNKNOWN) {
            try {
                boolean mechanicalExecutionProvablyNotStarted =
                        outcome.getPayload().path("startedStepIndex").isInt()
                                && outcome.getPayload().path("startedStepIndex").intValue() == -1;
                taskRunRegistry.completeGenericStep(
                        genericHandle, request.getSessionRef(),
                        mechanicalExecutionProvablyNotStarted);
            } catch (RuntimeException staleAfterInput) {
                return terminal(
                        command, registration, RemoteExecutionState.UNKNOWN,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic input completed but its post-execution fence changed",
                        timing,
                        RemoteInputBundleOutcomePayload.builder()
                                .actionCount(request.getActions().size())
                                .startedStepIndex(-1)
                                .lastCompletedStepIndex(-1)
                                .inputQueueRequestId(null)
                                .observedWindow(null)
                                .build());
            }
        }
        return outcome;
    }

    private RemoteGameOutcomeEnvelope executeInputBundleMechanical(
            RemoteGameCommand command,
            RemoteInputBundleCommandPayload request,
            RemoteTaskRunRegistry.InFlightExclusiveHandle genericHandle,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        boolean windowClientCoordinates = request.getCoordinateSpace() == RemoteCoordinateSpace.WINDOW_CLIENT_PX;
        final List<InputAction> screenAbsoluteActions;
        if (windowClientCoordinates) {
            screenAbsoluteActions = List.of();
        } else {
            validateInputCoordinates(request.getActions(), access.binding());
            screenAbsoluteActions = inputActionMapper.toInputActions(request.getActions());
        }

        InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(access.context(), () -> {
            requireRegistration(command, null, false);
            BindingAccess currentAccess = requireBoundWindow(command, true);
            requireRegistration(command, currentAccess.runner(), true);
            if (timing.timedOut()) {
                throw new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TIMEOUT,
                        "command timeout elapsed before input queue submission");
            }
            ClientInputGeometry inputGeometry = windowClientCoordinates
                    ? ClientInputGeometry.from(currentAccess.binding())
                    : null;
            List<RemoteInputActionDto> currentActions = windowClientCoordinates
                    ? toScreenAbsoluteInputActions(request.getActions(), inputGeometry)
                    : request.getActions();
            validateInputCoordinates(currentActions, currentAccess.binding());
            List<InputAction> actions = windowClientCoordinates
                    ? inputActionMapper.toInputActions(currentActions)
                    : screenAbsoluteActions;
            TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                            clientSession, command.getTaskRunId(), command.getWindow().getWindowId())
                    .orElseThrow(() -> new TerminalSignal(
                            RemoteExecutionState.NOT_EXECUTED,
                            RemoteOutcomeCode.TASK_RUN_MISMATCH,
                            "remote task run pause token is unavailable"));
            if (genericHandle != null) {
                return inputActionQueue.submitRetainedSessionStepAndWait(
                        taskRunRegistry.genericInputSession(genericHandle), actions);
            }
            return inputActionQueue.submitRemoteAndWaitDetailed(
                    request.getDescription(), actions, timing.deadlineNanos(), pauseToken,
                    () -> remoteInputSafetyReason(command, currentAccess.runner(), inputGeometry),
                    () -> workerAdmissionRevisionFence(
                            command, currentAccess.runner(), inputGeometry));
        });

        int startedStepIndex = executionResult.getStartedStepIndex();
        int lastCompletedStepIndex = executionResult.getLastCompletedStepIndex();
        boolean allStepsCompleted = executionResult.isCompleted()
                && lastCompletedStepIndex == request.getActions().size() - 1;
        if (!allStepsCompleted) {
            String reason = executionResult.getReason() == null ? "unknown" : executionResult.getReason();
            InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                    ? InputActionSafetyReason.CLEAR
                    : executionResult.getSafetyReason();
            boolean deadline = reason.startsWith("deadline-exceeded:");
            boolean bindingChanged = reason.startsWith("identity-suspended:")
                    || reason.startsWith("player-identity-epoch-changed:")
                    || reason.startsWith("native-binding-unavailable");
            boolean transportLost = reason.startsWith("waiter interrupted");
            RemoteExecutionState executionState;
            RemoteOutcomeCode outcomeCode;
            if (safetyReason.blocksInput()) {
                outcomeCode = switch (safetyReason) {
                    case STOP_REQUESTED -> RemoteOutcomeCode.STOP_REQUESTED;
                    case TASK_RUN_MISMATCH -> RemoteOutcomeCode.TASK_RUN_MISMATCH;
                    case WINDOW_BINDING_CHANGED -> RemoteOutcomeCode.WINDOW_BINDING_CHANGED;
                    case CLEAR -> throw new IllegalStateException("CLEAR is not a blocking safety reason");
                };
                if (startedStepIndex < 0) {
                    executionState = RemoteExecutionState.NOT_EXECUTED;
                } else if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
                    executionState = RemoteExecutionState.STOPPED;
                } else {
                    executionState = RemoteExecutionState.UNKNOWN;
                }
            } else if (startedStepIndex < 0) {
                executionState = RemoteExecutionState.NOT_EXECUTED;
                outcomeCode = deadline
                        ? RemoteOutcomeCode.TIMEOUT
                        : bindingChanged
                                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                                : transportLost
                                        ? RemoteOutcomeCode.TRANSPORT_LOST
                                        : RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
            } else {
                executionState = RemoteExecutionState.UNKNOWN;
                outcomeCode = deadline
                        ? RemoteOutcomeCode.TIMEOUT
                        : bindingChanged
                                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                                : transportLost
                                        ? RemoteOutcomeCode.TRANSPORT_LOST
                                        : RemoteOutcomeCode.INPUT_FAILED;
            }
            RemoteInputBundleOutcomePayload payload = RemoteInputBundleOutcomePayload.builder()
                    .actionCount(request.getActions().size())
                    .startedStepIndex(startedStepIndex)
                    .lastCompletedStepIndex(lastCompletedStepIndex)
                    .inputQueueRequestId(executionResult.getRequestId())
                    .observedWindow(null)
                    .build();
            return terminal(
                    command,
                    registration,
                    executionState,
                    outcomeCode,
                    "input queue " + executionResult.getStatus() + ": " + reason,
                    timing,
                    payload);
        }

        BindingAccess observedAccess;
        try {
            observedAccess = requireBoundWindow(command, true);
        } catch (TerminalSignal e) {
            RemoteInputBundleOutcomePayload payload = RemoteInputBundleOutcomePayload.builder()
                    .actionCount(request.getActions().size())
                    .startedStepIndex(startedStepIndex)
                    .lastCompletedStepIndex(lastCompletedStepIndex)
                    .inputQueueRequestId(executionResult.getRequestId())
                    .observedWindow(null)
                    .build();
            return terminal(
                    command,
                    registration,
                    RemoteExecutionState.UNKNOWN,
                    RemoteOutcomeCode.WINDOW_BINDING_CHANGED,
                    "input completed but post-execution binding changed",
                    timing,
                    payload);
        }
        RemoteInputBundleOutcomePayload payload = RemoteInputBundleOutcomePayload.builder()
                .actionCount(request.getActions().size())
                .startedStepIndex(startedStepIndex)
                .lastCompletedStepIndex(lastCompletedStepIndex)
                .inputQueueRequestId(executionResult.getRequestId())
                .observedWindow(observedWindow(observedAccess.context(), observedAccess.binding()))
                .build();
        return terminal(
                command,
                registration,
                RemoteExecutionState.EXECUTED,
                RemoteOutcomeCode.OK,
                "input bundle completed",
                timing,
                payload);
    }

    private RemoteGameOutcomeEnvelope executeExclusiveInteractionControl(
            RemoteGameCommand command,
            RemoteExclusiveInteractionControlCommandPayload request,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        if (request.getCommand()
                == RemoteExclusiveInteractionControlCommandPayload.Command.ACQUIRE) {
            RemoteTaskRunRegistry.InFlightExclusiveHandle handle;
            try {
                handle = taskRunRegistry.openGenericExclusive(
                        admissionSnapshot, command, request);
            } catch (IllegalStateException staleAcquire) {
                return terminal(
                        command, registration, RemoteExecutionState.NOT_EXECUTED,
                        RemoteOutcomeCode.TASK_RUN_MISMATCH,
                        "generic exclusive acquire fence rejected before enqueue: "
                                + staleAcquire.getMessage(),
                        timing, emptyControlPayload(command, RemoteExecutionState.NOT_EXECUTED));
            }
            boolean keepHandle = false;
            try {
                TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                                clientSession,
                                command.getTaskRunId(),
                                command.getWindow().getWindowId())
                        .orElseThrow(() -> new TerminalSignal(
                                RemoteExecutionState.NOT_EXECUTED,
                                RemoteOutcomeCode.TASK_RUN_MISMATCH,
                                "remote task run pause token is unavailable"));
                InputActionQueue.RetainedSessionHandle inputSession =
                        windowTaskContextHolder.callWith(
                                access.context(),
                                () -> inputActionQueue.openRetainedSession(
                                        "task-transaction:exclusive",
                                        pauseToken,
                                        () -> genericContinuationSafetyReason(
                                                handle, access.runner()),
                                        () -> exclusiveWorkerAdmissionReason(
                                                handle, command, access.runner())));
                taskRunRegistry.attachGenericInputSession(handle, inputSession);
                RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus status;
                RemoteExecutionState executionState;
                RemoteOutcomeCode code;
                boolean ownerReleased;
                switch (inputSession.admission()) {
                    case ADMITTED -> {
                        status = RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                                .ACQUIRED;
                        executionState = RemoteExecutionState.EXECUTED;
                        code = RemoteOutcomeCode.OK;
                        ownerReleased = false;
                        keepHandle = true;
                    }
                    case REJECTED_NOT_EXECUTED -> {
                        status = RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                                .NOT_EXECUTED;
                        executionState = RemoteExecutionState.NOT_EXECUTED;
                        code = RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
                        ownerReleased = true;
                    }
                    case ADMISSION_UNKNOWN -> {
                        status = RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                                .UNKNOWN;
                        executionState = RemoteExecutionState.UNKNOWN;
                        code = RemoteOutcomeCode.INPUT_FAILED;
                        ownerReleased = inputSession.releasedTerminalSnapshot() != null;
                        keepHandle = !ownerReleased;
                    }
                    default -> throw new IllegalStateException("unsupported session admission");
                }
                RemoteExclusiveInteractionControlOutcomePayload payload =
                        RemoteExclusiveInteractionControlOutcomePayload.builder()
                                .command(request.getCommand())
                                .exclusiveSessionId(request.getExclusiveSessionId())
                                .bindingGeneration(request.getBindingGeneration())
                                .step(request.getStep())
                                .mechanicalStatus(status)
                                .ownerReleased(ownerReleased)
                                .build();
                return terminal(
                        command, registration, executionState, code,
                        "generic exclusive acquire " + status, timing, payload);
            } finally {
                if (!keepHandle) {
                    taskRunRegistry.closeInFlightExclusive(handle);
                }
            }
        }

        RemoteTaskRunRegistry.InFlightExclusiveHandle handle;
        try {
            handle = taskRunRegistry.requireGenericExclusiveControl(
                    admissionSnapshot, command, request);
        } catch (IllegalStateException staleControl) {
            return terminal(
                    command, registration, RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TASK_RUN_MISMATCH,
                    "generic exclusive terminal fence rejected before execution: "
                            + staleControl.getMessage(),
                    timing, emptyControlPayload(command, RemoteExecutionState.NOT_EXECUTED));
        }
        InputActionQueue.SessionTerminalCommand terminalCommand =
                request.getCommand()
                        == RemoteExclusiveInteractionControlCommandPayload.Command.RELEASE
                        ? InputActionQueue.SessionTerminalCommand.RELEASE
                        : InputActionQueue.SessionTerminalCommand.ABORT;
        InputActionExecutionResult terminalResult =
                inputActionQueue.terminateRetainedSessionAndWait(
                        taskRunRegistry.genericInputSession(handle), terminalCommand);
        taskRunRegistry.retainGenericTerminalSnapshot(handle, terminalResult);
        RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus status;
        RemoteExecutionState executionState;
        RemoteOutcomeCode code;
        switch (terminalResult.getStatus()) {
            case COMPLETED -> {
                status = request.getCommand()
                        == RemoteExclusiveInteractionControlCommandPayload.Command.RELEASE
                        ? RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.RELEASED
                        : RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ABORTED;
                executionState = RemoteExecutionState.EXECUTED;
                code = RemoteOutcomeCode.OK;
            }
            case NOT_STARTED -> {
                boolean explicitlyStopped = terminalResult.getSafetyReason()
                        == InputActionSafetyReason.STOP_REQUESTED;
                status = explicitlyStopped
                        ? RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.STOPPED
                        : RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.NOT_EXECUTED;
                executionState = explicitlyStopped
                        ? RemoteExecutionState.STOPPED
                        : RemoteExecutionState.NOT_EXECUTED;
                code = retainedTerminalControlOutcomeCode(terminalResult);
            }
            case PARTIALLY_COMPLETED, STARTED_UNKNOWN -> {
                status = RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.UNKNOWN;
                executionState = RemoteExecutionState.UNKNOWN;
                code = retainedTerminalControlOutcomeCode(terminalResult);
            }
            default -> throw new IllegalStateException("unsupported retained terminal status");
        }
        RemoteExclusiveInteractionControlOutcomePayload payload =
                RemoteExclusiveInteractionControlOutcomePayload.builder()
                        .command(request.getCommand())
                        .exclusiveSessionId(request.getExclusiveSessionId())
                        .bindingGeneration(request.getBindingGeneration())
                        .step(request.getStep())
                        .mechanicalStatus(status)
                        .ownerReleased(true)
                        .build();
        taskRunRegistry.closeInFlightExclusive(handle);
        return terminal(
                command, registration, executionState, code,
                "generic exclusive " + request.getCommand() + " worker released as "
                        + status + ": "
                        + terminalResult.getReason(),
                timing, payload);
    }

    private InputActionSafetyReason genericContinuationSafetyReason(
            RemoteTaskRunRegistry.InFlightExclusiveHandle handle,
            WindowTaskRunner runner) {
        InputActionSafetyReason continuation = mapExclusiveCheck(
                taskRunRegistry.checkInFlightExclusive(handle));
        if (continuation.blocksInput()) {
            return continuation;
        }
        return runner == null
                ? InputActionSafetyReason.TASK_RUN_MISMATCH
                : InputActionSafetyReason.CLEAR;
    }

    private RemoteGameOutcomeEnvelope executeSummonSkillWholePass(
            RemoteGameCommand command,
            RemoteSummonSkillWholePassCommandPayload request,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot,
            RemoteTaskRunRegistration registration,
            BindingAccess access,
            OperationTiming timing) {
        RemoteTaskRunRegistry.InFlightExclusiveHandle handle =
                taskRunRegistry.openInFlightExclusive(
                        admissionSnapshot, command, request.getExclusiveSessionId());
        try {
            TaskPauseToken pauseToken = taskRunRegistry.pauseToken(
                            clientSession,
                            command.getTaskRunId(),
                            command.getWindow().getWindowId())
                    .orElseThrow(() -> new TerminalSignal(
                            RemoteExecutionState.NOT_EXECUTED,
                            RemoteOutcomeCode.TASK_RUN_MISMATCH,
                            "remote task run pause token is unavailable"));
            SummonSkillCleanupRequest cleanupRequest = SummonSkillCleanupRequest.builder()
                    .expectedSkillCount(request.getExpectedSkillCount())
                    .trustExpectedSkillCount(request.isTrustExpectedSkillCount())
                    .startSlotIndex(request.getStartSlotIndex())
                    .skipUltimateCornerCheck(request.isSkipUltimateCornerCheck())
                    .build();
            SummonSkillCleanupResult[] callbackResult = new SummonSkillCleanupResult[1];

            InputActionExecutionResult executionResult = windowTaskContextHolder.callWith(
                    access.context(),
                    () -> inputActionQueue.submitRemoteExclusiveAndWaitDetailed(
                            "summon-skill:whole-pass",
                            () -> {
                                callbackResult[0] = summonSkillService.cleanSummonSkillsOnce(
                                        cleanupRequest);
                                return true;
                            },
                            timing.deadlineNanos(),
                            pauseToken,
                            () -> exclusiveContinuationSafetyReason(
                                    handle, command, access.runner()),
                            () -> exclusiveWorkerAdmissionReason(
                                    handle, command, access.runner())));

            boolean callbackStarted = executionResult.getStartedStepIndex() >= 0;
            boolean ownerNeverAcquired = !callbackStarted;
            boolean ownerReleased = callbackStarted;
            RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus status;
            RemoteExecutionState executionState;
            RemoteOutcomeCode outcomeCode;
            RemoteSummonSkillWholePassOutcomePayload.CleanupValue cleanupValue = null;
            String message;

            InputActionSafetyReason safetyReason = executionResult.getSafetyReason() == null
                    ? InputActionSafetyReason.CLEAR
                    : executionResult.getSafetyReason();
            if (executionResult.isCompleted() && callbackResult[0] != null) {
                status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.EXECUTED;
                executionState = RemoteExecutionState.EXECUTED;
                outcomeCode = RemoteOutcomeCode.OK;
                cleanupValue = cleanupValue(callbackResult[0]);
                message = "summon-skill whole pass executed";
            } else if (safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
                status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.STOPPED;
                executionState = RemoteExecutionState.STOPPED;
                outcomeCode = RemoteOutcomeCode.STOP_REQUESTED;
                message = "summon-skill whole pass stopped: " + executionResult.getReason();
            } else if (!callbackStarted) {
                status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.NOT_EXECUTED;
                executionState = RemoteExecutionState.NOT_EXECUTED;
                outcomeCode = outcomeCodeForUnstarted(executionResult, safetyReason);
                message = "summon-skill whole pass not executed: " + executionResult.getReason();
            } else {
                status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.UNKNOWN;
                executionState = RemoteExecutionState.UNKNOWN;
                outcomeCode = safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                        ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                        : safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH
                                ? RemoteOutcomeCode.TASK_RUN_MISMATCH
                                : RemoteOutcomeCode.INPUT_FAILED;
                message = "summon-skill whole pass became mechanically uncertain: "
                        + executionResult.getReason();
            }

            if (status == RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.EXECUTED) {
                try {
                    requireBoundWindow(command, true);
                } catch (TerminalSignal bindingFailure) {
                    status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.UNKNOWN;
                    executionState = RemoteExecutionState.UNKNOWN;
                    outcomeCode = RemoteOutcomeCode.WINDOW_BINDING_CHANGED;
                    cleanupValue = null;
                    message = "whole pass completed but the exact window binding changed";
                }
            }

            boolean deterministicNoOwner = status
                    != RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.UNKNOWN
                    && (ownerNeverAcquired || ownerReleased);
            if (deterministicNoOwner) {
                try {
                    uiCleanerService.cleanLightweightInterruptions("summon-skill:finish");
                } catch (RuntimeException cleanerFailure) {
                    status = RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.UNKNOWN;
                    executionState = RemoteExecutionState.UNKNOWN;
                    outcomeCode = RemoteOutcomeCode.INTERNAL_ERROR;
                    cleanupValue = null;
                    message = "post-pass UI cleaner failed: " + safeMessage(cleanerFailure);
                }
            }

            RemoteSummonSkillWholePassOutcomePayload payload =
                    RemoteSummonSkillWholePassOutcomePayload.builder()
                            .mechanicalStatus(status)
                            .cleanupResult(cleanupValue)
                            .callbackStarted(callbackStarted)
                            .ownerNeverAcquired(ownerNeverAcquired)
                            .ownerReleased(ownerReleased)
                            .build();
            return terminal(
                    command,
                    registration,
                    executionState,
                    outcomeCode,
                    message,
                    timing,
                    payload);
        } finally {
            taskRunRegistry.closeInFlightExclusive(handle);
        }
    }

    private InputActionSafetyReason exclusiveWorkerAdmissionReason(
            RemoteTaskRunRegistry.InFlightExclusiveHandle handle,
            RemoteGameCommand command,
            WindowTaskRunner runner) {
        InputActionSafetyReason continuation = mapExclusiveCheck(
                taskRunRegistry.admitInFlightExclusive(handle, command));
        if (continuation.blocksInput()) {
            return continuation;
        }
        return remoteInputSafetyReason(command, runner, null);
    }

    private InputActionSafetyReason exclusiveContinuationSafetyReason(
            RemoteTaskRunRegistry.InFlightExclusiveHandle handle,
            RemoteGameCommand command,
            WindowTaskRunner runner) {
        InputActionSafetyReason continuation = mapExclusiveCheck(
                taskRunRegistry.checkInFlightExclusive(handle));
        if (continuation.blocksInput()) {
            return continuation;
        }
        return remoteInputSafetyReason(command, runner, null);
    }

    private static InputActionSafetyReason mapExclusiveCheck(
            RemoteTaskRunRegistry.InFlightExclusiveCheck check) {
        return switch (check) {
            case CLEAR, PAUSED -> InputActionSafetyReason.CLEAR;
            case STOPPED -> InputActionSafetyReason.STOP_REQUESTED;
            case MISMATCH -> InputActionSafetyReason.TASK_RUN_MISMATCH;
        };
    }

    private static RemoteOutcomeCode outcomeCodeForUnstarted(
            InputActionExecutionResult executionResult,
            InputActionSafetyReason safetyReason) {
        if (safetyReason == InputActionSafetyReason.WINDOW_BINDING_CHANGED) {
            return RemoteOutcomeCode.WINDOW_BINDING_CHANGED;
        }
        if (safetyReason == InputActionSafetyReason.TASK_RUN_MISMATCH) {
            return RemoteOutcomeCode.TASK_RUN_MISMATCH;
        }
        String reason = executionResult.getReason() == null ? "" : executionResult.getReason();
        return reason.startsWith("deadline-exceeded:")
                ? RemoteOutcomeCode.TIMEOUT
                : RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
    }

    private static RemoteSummonSkillWholePassOutcomePayload.CleanupValue cleanupValue(
            SummonSkillCleanupResult result) {
        Map<Integer, RemoteSummonSkillWholePassOutcomePayload.SlotStatus> statuses =
                new LinkedHashMap<>();
        for (Map.Entry<Integer, SummonSkillSlotStatus> entry
                : result.getObservedStatusesByIndex().entrySet()) {
            statuses.put(
                    entry.getKey(),
                    RemoteSummonSkillWholePassOutcomePayload.SlotStatus.valueOf(
                            entry.getValue().name()));
        }
        return RemoteSummonSkillWholePassOutcomePayload.CleanupValue.builder()
                .success(result.isSuccess())
                .skillCount(result.getSkillCount())
                .nextStartIndex(result.getNextStartIndex())
                .observedSlotStatuses(statuses)
                .ultimateSkillClicked(result.isUltimateGenerateClicked())
                .ultimateSkillSucceeded(result.isUltimateGenerateSucceeded())
                .inspectedSlotCount(result.getInspectedCount())
                .deletedSkillCount(result.getDeletedCount())
                .message(result.getMessage())
                .build();
    }

    private RemoteTaskRunRegistration requireRegistration(
            RemoteGameCommand command,
            WindowTaskRunner runner,
            boolean lifecycleGate) {
        RemoteTaskRunRegistration registration = taskRunRegistry.find(command.getTaskRunId()).orElse(null);
        RemoteRunCorrelation correlation = classifyRemoteRun(
                command, registration, runner, lifecycleGate);
        return switch (correlation) {
            case CORRELATED, ACTIVE -> {
                // Pre-side-effect revision gate: a request built against an older coordinator
                // revision must never execute locally, even when window and stopEpoch still
                // match. Deliberately not applied to the in-flight safety supplier so an
                // already-started input bundle keeps the existing pause-token behavior.
                if (command.getRunRevision() == null
                        || registration.getRunRevision() != command.getRunRevision()) {
                    throw new TerminalSignal(
                            RemoteExecutionState.NOT_EXECUTED,
                            RemoteOutcomeCode.TASK_RUN_MISMATCH,
                            "command runRevision does not match local task run registration");
                }
                yield registration;
            }
            case PAUSED -> throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TASK_RUN_PAUSED,
                    "remote task run is paused");
            case STOP_REQUESTED -> throw new TerminalSignal(
                    RemoteExecutionState.STOPPED,
                    RemoteOutcomeCode.STOP_REQUESTED,
                    "remote or local task stop is active");
            case TASK_RUN_MISMATCH -> throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.TASK_RUN_MISMATCH,
                    "remote task run owner, state, or stop epoch is mismatched");
            case WINDOW_BINDING_CHANGED -> throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.WINDOW_BINDING_CHANGED,
                    "remote task run window binding changed");
        };
    }

    private BindingAccess requireBoundWindow(RemoteGameCommand command, boolean changedDuringOperation) {
        RemoteOutcomeCode mismatchCode = changedDuringOperation
                ? RemoteOutcomeCode.WINDOW_BINDING_CHANGED
                : RemoteOutcomeCode.WRONG_WINDOW;
        WindowTaskRunner runner = multiWindowTaskManager
                .getRunner(command.getWindow().getWindowId())
                .orElseThrow(() -> new TerminalSignal(
                        RemoteExecutionState.NOT_EXECUTED,
                        mismatchCode,
                        "registered window runner was not found"));
        if (runner.isShutdown()) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    mismatchCode,
                    "registered window runner is shut down");
        }
        WindowRuntimeContext context = runner.getWindowContext();
        if (context == null || !Objects.equals(context.getWindowId(), command.getWindow().getWindowId())) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    mismatchCode,
                    "window runtime context does not match command windowId");
        }
        if (bindingRefreshService.refreshAndCommit(context).isEmpty()) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    mismatchCode,
                    "bound HWND is unavailable");
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null
                || !binding.hasNativeHandle()
                || !Objects.equals(binding.getNativeHandle(), command.getWindow().getNativeHandle())
                || binding.getProcessId() != command.getWindow().getProcessId()
                || context.getPlayerIdentityEpoch() != command.getWindow().getPlayerIdentityEpoch()) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    mismatchCode,
                    "nativeHandle/processId/playerIdentityEpoch mismatch");
        }
        return new BindingAccess(runner, context, binding);
    }

    private RemoteTaskRunRegistration matchingRegistration(RemoteGameCommand command) {
        if (command == null || command.getWindow() == null || command.getStop() == null) {
            return null;
        }
        RemoteTaskRunRegistration registration = taskRunRegistry.find(command.getTaskRunId()).orElse(null);
        if (registration == null
                || !registration.getTenantId().equals(clientSession.getTenantId())
                || !registration.getUserId().equals(clientSession.getUserId())
                || !registration.getDeviceId().equals(clientSession.getDeviceId())
                || !registration.getClientSessionId().equals(clientSession.getClientSessionId())
                || !registration.getWindowId().equals(command.getWindow().getWindowId())
                || !registration.getNativeHandle().equals(command.getWindow().getNativeHandle())
                || registration.getProcessId() != command.getWindow().getProcessId()
                || registration.getPlayerIdentityEpoch() != command.getWindow().getPlayerIdentityEpoch()
                || !registration.getTaskRunId().equals(command.getTaskRunId())
                || registration.getStopEpoch() != command.getStop().getStopEpoch()) {
            return null;
        }
        return registration;
    }

    private RemoteGameOutcomeEnvelope terminal(
            RemoteGameCommand command,
            RemoteTaskRunRegistration registration,
            RemoteExecutionState executionState,
            RemoteOutcomeCode code,
            String message,
            OperationTiming timing,
            Object operationPayload) {
        RemoteClientSessionRef scope = registration == null
                ? clientSession
                : RemoteClientSessionRef.builder()
                        .tenantId(registration.getTenantId())
                        .userId(registration.getUserId())
                        .deviceId(registration.getDeviceId())
                        .clientSessionId(registration.getClientSessionId())
                        .build();
        long finishedAtEpochMs = Math.max(timing.acceptedAtEpochMs(), System.currentTimeMillis());
        Object exactPayload = operationPayload;
        if (command.getOperation() == RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS) {
            RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus expectedStatus =
                    mechanicalStatus(executionState);
            if (!(operationPayload instanceof RemoteSummonSkillWholePassOutcomePayload wholePass)
                    || wholePass.getMechanicalStatus() != expectedStatus) {
                if (expectedStatus
                        == RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.EXECUTED) {
                    executionState = RemoteExecutionState.UNKNOWN;
                    code = RemoteOutcomeCode.INTERNAL_ERROR;
                    message = "whole-pass executed outcome lacks exact cleanup proof";
                }
                exactPayload = emptyWholePassPayload(executionState);
            }
        } else if (command.getOperation()
                == RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL) {
            RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus expectedStatus =
                    controlMechanicalStatus(command, executionState);
            if (!(operationPayload instanceof RemoteExclusiveInteractionControlOutcomePayload control)
                    || control.getMechanicalStatus() != expectedStatus) {
                if (executionState == RemoteExecutionState.EXECUTED) {
                    executionState = RemoteExecutionState.UNKNOWN;
                    code = RemoteOutcomeCode.INTERNAL_ERROR;
                    message = "exclusive control executed outcome lacks exact terminal proof";
                }
                exactPayload = emptyControlPayload(command, executionState);
            }
        }
        RemoteGameOutcomeEnvelope unsigned = RemoteGameOutcomeEnvelope.builder()
                .contractVersion(command.getContractVersion())
                .tenantId(scope.getTenantId())
                .userId(scope.getUserId())
                .deviceId(scope.getDeviceId())
                .clientSessionId(scope.getClientSessionId())
                .operation(command.getOperation())
                .requestId(command.getRequestId())
                .actionId(command.getActionId())
                .taskRunId(command.getTaskRunId())
                .semanticAddress(command.getSemanticAddress())
                .requestDigest(command.getRequestDigest())
                .outcomeDigest(RemoteProtocolDigests.ZERO_SHA256)
                .executionState(executionState)
                .code(code)
                .message(message)
                .acceptedAtEpochMs(timing.acceptedAtEpochMs())
                .finishedAtEpochMs(finishedAtEpochMs)
                .payload(payloadCodec.toPayloadTree(exactPayload))
                .build();
        return unsigned.toBuilder()
                .outcomeDigest(protocolDigests.computeOutcomeDigest(unsigned))
                .build();
    }

    private Object emptyOutcomePayload(RemoteGameCommand command) {
        return switch (command.getOperation()) {
            case CAPTURE -> emptyCapturePayload(safeCaptureId(command));
            case WINDOW_FACT -> RemoteWindowFactOutcomePayload.builder()
                    .factKind(safeFactKind(command))
                    .fact(null)
                    .build();
            case EXECUTE_INPUT_BUNDLE -> RemoteInputBundleOutcomePayload.builder()
                    .actionCount(safeActionCount(command))
                    .startedStepIndex(-1)
                    .lastCompletedStepIndex(-1)
                    .inputQueueRequestId(null)
                    .observedWindow(null)
                    .build();
            case EXCLUSIVE_INTERACTION_CONTROL ->
                    emptyControlPayload(command, RemoteExecutionState.UNKNOWN);
            case SUMMON_SKILL_WHOLE_PASS ->
                    emptyWholePassPayload(RemoteExecutionState.UNKNOWN);
            case TASK_TRACKER_READ -> RemoteTaskTrackerReadOutcomePayload.builder()
                    .captureId(safeCaptureId(command))
                    .readProfile(null)
                    .source(null)
                    .artifact(null)
                    .frames(null)
                    .mechanicalFact(null)
                    .observedWindow(null)
                    .build();
            case TASK_TRACKER_MATERIALIZE_ACTION ->
                    RemoteTaskTrackerMaterializeOutcomePayload.builder()
                            .artifact(null)
                            .observationDigest(null)
                            .preparedActionId(null)
                            .publishDisposition(null)
                            .validationFingerprintDigest(null)
                            .observedWindow(null)
                            .build();
            case LOCAL_MACRO -> {
                // Cloud's strict localMacroOutcome parser requires the closed flat shape on every
                // terminal, including STOPPED/NOT_EXECUTED/UNKNOWN. Preserve the requested closed
                // macroKind and keep every typed-result field explicitly null; never fabricate a
                // business observation for a transport terminal.
                java.util.Map<String, Object> outcome = new java.util.LinkedHashMap<>();
                outcome.put("macroKind", safeLocalMacroKind(command));
                outcome.put("operation", null);
                outcome.put("state", null);
                outcome.put("cachePoint", null);
                yield outcome;
            }
        };
    }

    private RemoteExclusiveInteractionControlOutcomePayload emptyControlPayload(
            RemoteGameCommand command,
            RemoteExecutionState executionState) {
        RemoteExclusiveInteractionControlCommandPayload control;
        try {
            control = payloadCodec.readExclusiveInteractionControl(command.getPayload());
        } catch (RuntimeException invalid) {
            control = RemoteExclusiveInteractionControlCommandPayload.builder()
                    .command(RemoteExclusiveInteractionControlCommandPayload.Command.ACQUIRE)
                    .exclusiveSessionId("invalid-exclusive-session")
                    .bindingGeneration(0L)
                    .step(1L)
                    .build();
        }
        RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus status =
                controlMechanicalStatus(control.getCommand(), executionState);
        boolean ownerReleased = status
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.RELEASED
                || status == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ABORTED
                || control.getCommand()
                        == RemoteExclusiveInteractionControlCommandPayload.Command.ACQUIRE
                        && (status == RemoteExclusiveInteractionControlOutcomePayload
                                .MechanicalStatus.NOT_EXECUTED
                                || status == RemoteExclusiveInteractionControlOutcomePayload
                                        .MechanicalStatus.STOPPED);
        return RemoteExclusiveInteractionControlOutcomePayload.builder()
                .command(control.getCommand())
                .exclusiveSessionId(control.getExclusiveSessionId())
                .bindingGeneration(control.getBindingGeneration())
                .step(control.getStep())
                .mechanicalStatus(status)
                .ownerReleased(ownerReleased)
                .build();
    }

    private RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
            controlMechanicalStatus(
                    RemoteGameCommand command,
                    RemoteExecutionState executionState) {
        RemoteExclusiveInteractionControlCommandPayload.Command controlCommand;
        try {
            controlCommand = payloadCodec.readExclusiveInteractionControl(
                    command.getPayload()).getCommand();
        } catch (RuntimeException invalid) {
            controlCommand = RemoteExclusiveInteractionControlCommandPayload.Command.ACQUIRE;
        }
        return controlMechanicalStatus(controlCommand, executionState);
    }

    private static RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
            controlMechanicalStatus(
                    RemoteExclusiveInteractionControlCommandPayload.Command command,
                    RemoteExecutionState executionState) {
        return switch (executionState) {
            case EXECUTED -> switch (command) {
                case ACQUIRE -> RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                        .ACQUIRED;
                case RELEASE -> RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                        .RELEASED;
                case ABORT -> RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                        .ABORTED;
            };
            case NOT_EXECUTED -> RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus
                    .NOT_EXECUTED;
            case STOPPED -> RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.STOPPED;
            case UNKNOWN, OBSERVED ->
                    RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.UNKNOWN;
        };
    }

    private static RemoteSummonSkillWholePassOutcomePayload emptyWholePassPayload(
            RemoteExecutionState executionState) {
        RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus status =
                mechanicalStatus(executionState);
        if (status == RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.EXECUTED) {
            throw new IllegalArgumentException(
                    "EXECUTED whole-pass requires an exact cleanup payload");
        }
        boolean ownerNeverAcquired = status
                == RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.NOT_EXECUTED
                || status == RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.STOPPED;
        return RemoteSummonSkillWholePassOutcomePayload.builder()
                .mechanicalStatus(status)
                .cleanupResult(null)
                .callbackStarted(false)
                .ownerNeverAcquired(ownerNeverAcquired)
                .ownerReleased(false)
                .build();
    }

    private static RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus mechanicalStatus(
            RemoteExecutionState executionState) {
        return switch (executionState) {
            case EXECUTED ->
                    RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.EXECUTED;
            case NOT_EXECUTED ->
                    RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.NOT_EXECUTED;
            case STOPPED ->
                    RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.STOPPED;
            case UNKNOWN ->
                    RemoteSummonSkillWholePassOutcomePayload.MechanicalStatus.UNKNOWN;
            case OBSERVED -> throw new IllegalArgumentException(
                    "summon skill whole pass cannot use OBSERVED");
        };
    }

    private static RemoteCaptureOutcomePayload emptyCapturePayload(String captureId) {
        return RemoteCaptureOutcomePayload.builder()
                .captureId(captureId)
                .imageBytes(null)
                .imageSha256(null)
                .width(null)
                .height(null)
                .captureProvider(null)
                .systemScaleRatio(null)
                .observedWindow(null)
                .build();
    }

    private static RemoteInputBundleOutcomePayload emptyInputPayload(int actionCount) {
        return RemoteInputBundleOutcomePayload.builder()
                .actionCount(actionCount)
                .startedStepIndex(-1)
                .lastCompletedStepIndex(-1)
                .inputQueueRequestId(null)
                .observedWindow(null)
                .build();
    }

    private static RemoteOutcomeCode retainedTerminalOutcomeCode(
            InputActionExecutionResult terminalSnapshot) {
        if (terminalSnapshot == null) {
            return RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
        }
        String reason = terminalSnapshot.getReason();
        if (reason != null && reason.startsWith("deadline-exceeded:")) {
            return RemoteOutcomeCode.TIMEOUT;
        }
        InputActionSafetyReason safetyReason = terminalSnapshot.getSafetyReason();
        if (safetyReason == null) {
            return RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
        }
        return switch (safetyReason) {
            case STOP_REQUESTED -> RemoteOutcomeCode.STOP_REQUESTED;
            case TASK_RUN_MISMATCH -> RemoteOutcomeCode.TASK_RUN_MISMATCH;
            case WINDOW_BINDING_CHANGED -> RemoteOutcomeCode.WINDOW_BINDING_CHANGED;
            case CLEAR -> RemoteOutcomeCode.INPUT_QUEUE_REJECTED;
        };
    }

    private static RemoteOutcomeCode retainedTerminalControlOutcomeCode(
            InputActionExecutionResult terminalSnapshot) {
        String reason = terminalSnapshot.getReason();
        if (reason != null && reason.startsWith("deadline-exceeded:")) {
            return RemoteOutcomeCode.TIMEOUT;
        }
        if (reason != null && (reason.startsWith("waiter interrupted")
                || reason.startsWith("worker-interrupted:"))) {
            return RemoteOutcomeCode.TRANSPORT_LOST;
        }
        InputActionSafetyReason safetyReason = terminalSnapshot.getSafetyReason();
        if (safetyReason != null && safetyReason.blocksInput()) {
            return switch (safetyReason) {
                case STOP_REQUESTED -> RemoteOutcomeCode.STOP_REQUESTED;
                case TASK_RUN_MISMATCH -> RemoteOutcomeCode.TASK_RUN_MISMATCH;
                case WINDOW_BINDING_CHANGED -> RemoteOutcomeCode.WINDOW_BINDING_CHANGED;
                case CLEAR -> throw new IllegalStateException("CLEAR is not a blocking safety reason");
            };
        }
        return terminalSnapshot.getStatus() == InputActionExecutionResult.Status.NOT_STARTED
                ? RemoteOutcomeCode.INPUT_QUEUE_REJECTED
                : RemoteOutcomeCode.INPUT_FAILED;
    }

    // Reads the live system scale ratio right now, using the same AWT source as the pushed
    // CoordinateHelper#initScaleRatio() baseline. It never reads a cache, never reverses it from
    // geometry/image, and never falls back to 1.0; an unreadable value returns null.
    private static Double readSystemScaleRatioNow() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration()
                    .getDefaultTransform()
                    .getScaleX();
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean isValidSystemScaleRatio(Double value) {
        return value != null && Double.isFinite(value) && value > 0.0d;
    }

    private static String safeCaptureId(RemoteGameCommand command) {
        if (command.getPayload() != null && command.getPayload().path("captureId").isTextual()) {
            String captureId = command.getPayload().path("captureId").asText();
            if (!captureId.isBlank()) {
                return captureId;
            }
        }
        return command.getRequestId();
    }

    private static RemoteWindowFactKind safeFactKind(RemoteGameCommand command) {
        if (command.getPayload() != null && command.getPayload().path("factKind").isTextual()) {
            try {
                return RemoteWindowFactKind.valueOf(command.getPayload().path("factKind").asText());
            } catch (IllegalArgumentException ignored) {
                // BINDING keeps an INVALID_REQUEST outcome structurally decodable without claiming a fact.
            }
        }
        return RemoteWindowFactKind.BINDING;
    }

    private static RemoteLocalMacroKind safeLocalMacroKind(RemoteGameCommand command) {
        if (command.getPayload() != null && command.getPayload().path("macroKind").isTextual()) {
            try {
                return RemoteLocalMacroKind.valueOf(
                        command.getPayload().path("macroKind").asText());
            } catch (IllegalArgumentException ignored) {
                // Keep an INVALID_REQUEST terminal structurally decodable without running a macro.
            }
        }
        return RemoteLocalMacroKind.BAG_RETURN_ITEM;
    }

    private static int safeActionCount(RemoteGameCommand command) {
        if (command.getPayload() != null && command.getPayload().path("actions").isArray()) {
            return Math.max(1, command.getPayload().path("actions").size());
        }
        return 1;
    }

    private static CaptureRectangle captureRectangle(
            RemoteCaptureRegion region,
            WindowNativeBinding binding) {
        int x1 = region.getCoordinateSpace() == RemoteCoordinateSpace.WINDOW_CLIENT_PX
                ? Math.addExact(binding.getX(), region.getX())
                : region.getX();
        int y1 = region.getCoordinateSpace() == RemoteCoordinateSpace.WINDOW_CLIENT_PX
                ? Math.addExact(binding.getY(), region.getY())
                : region.getY();
        int x2 = Math.addExact(x1, region.getWidth());
        int y2 = Math.addExact(y1, region.getHeight());
        requirePointInside(binding, x1, y1, "capture top-left");
        requirePointInside(binding, x2 - 1, y2 - 1, "capture bottom-right");
        return new CaptureRectangle(x1, y1, x2, y2, region.getWidth(), region.getHeight());
    }

    private static void validateInputCoordinates(
            List<RemoteInputActionDto> actions,
            WindowNativeBinding binding) {
        for (RemoteInputActionDto action : actions) {
            switch (action.getType()) {
                case CLICK_LEFT, CLICK_RIGHT, DOUBLE_RIGHT_CLICK, MOVE_MOUSE ->
                        requirePointInside(binding, action.getX(), action.getY(), action.getType().name());
                case DRAG_AND_DROP -> {
                    requirePointInside(binding, action.getX(), action.getY(), "DRAG_AND_DROP start");
                    requirePointInside(binding, action.getEndX(), action.getEndY(), "DRAG_AND_DROP end");
                }
                default -> {
                }
            }
        }
    }

    /**
     * Converts one client-relative input bundle to screen-absolute coordinates using the exact
     * current binding admitted inside {@code callWith}.
     *
     * @param actions validated typed actions; coordinate values are client-relative pixels when used
     * @param inputGeometry immutable x/y/width/height snapshot from the current exact HWND binding
     * @return copy of {@code actions} with only pointer-action coordinates converted to screen pixels
     */
    private static List<RemoteInputActionDto> toScreenAbsoluteInputActions(
            List<RemoteInputActionDto> actions,
            ClientInputGeometry inputGeometry) {
        List<RemoteInputActionDto> converted = new ArrayList<>(actions.size());
        for (RemoteInputActionDto action : actions) {
            Integer x = action.getX();
            Integer y = action.getY();
            Integer endX = action.getEndX();
            Integer endY = action.getEndY();
            switch (action.getType()) {
                case CLICK_LEFT, CLICK_RIGHT, DOUBLE_RIGHT_CLICK, MOVE_MOUSE -> {
                    x = addWindowOrigin(inputGeometry.x(), x, action.getType() + ".x");
                    y = addWindowOrigin(inputGeometry.y(), y, action.getType() + ".y");
                }
                case DRAG_AND_DROP -> {
                    x = addWindowOrigin(inputGeometry.x(), x, "DRAG_AND_DROP.x");
                    y = addWindowOrigin(inputGeometry.y(), y, "DRAG_AND_DROP.y");
                    endX = addWindowOrigin(inputGeometry.x(), endX, "DRAG_AND_DROP.endX");
                    endY = addWindowOrigin(inputGeometry.y(), endY, "DRAG_AND_DROP.endY");
                }
                default -> {
                    // Key, text, scroll, and sleep actions have no coordinate payload.
                }
            }
            converted.add(RemoteInputActionDto.builder()
                    .type(action.getType())
                    .x(x)
                    .y(y)
                    .endX(endX)
                    .endY(endY)
                    .delayMs(action.getDelayMs())
                    .intervalMs(action.getIntervalMs())
                    .clicks(action.getClicks())
                    .text(action.getText())
                    .build());
        }
        return List.copyOf(converted);
    }

    private static int addWindowOrigin(int origin, Integer clientCoordinate, String label) {
        try {
            return Math.addExact(origin, clientCoordinate);
        } catch (ArithmeticException e) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    label + " overflows screen-absolute coordinates");
        }
    }

    private static void requirePointInside(WindowNativeBinding binding, int x, int y, String label) {
        if (binding == null || !binding.hasGeometry()) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.WRONG_WINDOW,
                    "window geometry is unavailable for " + label);
        }
        long right = (long) binding.getX() + binding.getWidth();
        long bottom = (long) binding.getY() + binding.getHeight();
        if (x < binding.getX() || y < binding.getY() || x >= right || y >= bottom) {
            throw new TerminalSignal(
                    RemoteExecutionState.NOT_EXECUTED,
                    RemoteOutcomeCode.INVALID_REQUEST,
                    label + " is outside the bound window");
        }
    }

    private RemoteFocusState focusState(WindowNativeBinding binding) {
        String foregroundHandle = windowFocusService.getForegroundNativeHandleText();
        if (foregroundHandle == null || foregroundHandle.isBlank()) {
            return RemoteFocusState.UNKNOWN;
        }
        Long foreground = WindowHandleParser.parseHandle(foregroundHandle);
        Long expected = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (foreground == null || expected == null) {
            return RemoteFocusState.UNKNOWN;
        }
        return foreground.equals(expected) ? RemoteFocusState.FOREGROUND : RemoteFocusState.BACKGROUND;
    }

    private static boolean localStopRequested(WindowTaskRunner runner) {
        RunningTaskHandle currentTask = runner == null ? null : runner.getCurrentTask();
        TaskStopToken stopToken = currentTask == null ? null : currentTask.getStopToken();
        return currentTask != null
                && currentTask.isRunning()
                && stopToken != null
                && stopToken.isStopRequested();
    }

    /**
     * One-shot worker-admission fence, evaluated after the queue pause wait and immediately before
     * focus/first physical step.
     *
     * @param command remote command whose revision must still match the local registration
     * @param runner exact logical-window runner captured after the callWith-side binding fence
     * @param inputGeometry null for screen-absolute input; otherwise the conversion binding's exact
     *                      x/y/width/height snapshot, which must still match before first input
     * @return CLEAR when admission may proceed, otherwise a typed reason that prevents the first step
     */
    private InputActionSafetyReason workerAdmissionRevisionFence(
            RemoteGameCommand command,
            WindowTaskRunner runner,
            ClientInputGeometry inputGeometry) {
        RemoteTaskRunRegistration current = taskRunRegistry.find(command.getTaskRunId()).orElse(null);
        boolean revisionMatches = current != null
                && command.getRunRevision() != null
                && current.getRunRevision() == command.getRunRevision();
        if (!revisionMatches) {
            return InputActionSafetyReason.TASK_RUN_MISMATCH;
        }
        return inputGeometry == null || inputGeometry.matches(runner)
                ? InputActionSafetyReason.CLEAR
                : InputActionSafetyReason.WINDOW_BINDING_CHANGED;
    }

    private InputActionSafetyReason remoteInputSafetyReason(
            RemoteGameCommand command,
            WindowTaskRunner runner,
            ClientInputGeometry inputGeometry) {
        RemoteTaskRunRegistration current = taskRunRegistry.find(command.getTaskRunId()).orElse(null);
        return switch (classifyRemoteRun(command, current, runner, true)) {
            case ACTIVE, PAUSED -> inputGeometry == null || inputGeometry.matches(runner)
                    ? InputActionSafetyReason.CLEAR
                    : InputActionSafetyReason.WINDOW_BINDING_CHANGED;
            case STOP_REQUESTED -> InputActionSafetyReason.STOP_REQUESTED;
            case TASK_RUN_MISMATCH -> InputActionSafetyReason.TASK_RUN_MISMATCH;
            case WINDOW_BINDING_CHANGED -> InputActionSafetyReason.WINDOW_BINDING_CHANGED;
            case CORRELATED -> throw new IllegalStateException(
                    "full remote input classification cannot be correlation-only");
        };
    }

    private RemoteRunCorrelation classifyRemoteRun(
            RemoteGameCommand command,
            RemoteTaskRunRegistration registration,
            WindowTaskRunner runner,
            boolean lifecycleGate) {
        if (command == null
                || command.getWindow() == null
                || registration == null
                || command.getStop() == null
                || !registration.getTenantId().equals(clientSession.getTenantId())
                || !registration.getUserId().equals(clientSession.getUserId())
                || !registration.getDeviceId().equals(clientSession.getDeviceId())
                || !registration.getClientSessionId().equals(clientSession.getClientSessionId())
                || !registration.getTaskRunId().equals(command.getTaskRunId())
                || !registration.getTaskRunId().equals(command.getStop().getTaskRunId())) {
            return RemoteRunCorrelation.TASK_RUN_MISMATCH;
        }
        if (!registration.getWindowId().equals(command.getWindow().getWindowId())
                || !registration.getNativeHandle().equals(command.getWindow().getNativeHandle())
                || registration.getProcessId() != command.getWindow().getProcessId()
                || registration.getPlayerIdentityEpoch() != command.getWindow().getPlayerIdentityEpoch()) {
            return RemoteRunCorrelation.WINDOW_BINDING_CHANGED;
        }
        if (!lifecycleGate) {
            return RemoteRunCorrelation.CORRELATED;
        }
        if (runner == null || runner.isShutdown()) {
            return RemoteRunCorrelation.WINDOW_BINDING_CHANGED;
        }
        WindowRuntimeContext context = runner.getWindowContext();
        WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
        if (context == null
                || !Objects.equals(context.getWindowId(), command.getWindow().getWindowId())
                || binding == null
                || !binding.hasNativeHandle()
                || !Objects.equals(binding.getNativeHandle(), command.getWindow().getNativeHandle())
                || binding.getProcessId() != command.getWindow().getProcessId()
                || context.getPlayerIdentityEpoch() != command.getWindow().getPlayerIdentityEpoch()) {
            return RemoteRunCorrelation.WINDOW_BINDING_CHANGED;
        }
        if (registration.getStatus() == RemoteTaskRunStatus.STOPPING
                || registration.getStatus().isTerminal()
                || localStopRequested(runner)) {
            return RemoteRunCorrelation.STOP_REQUESTED;
        }
        if (registration.getStatus() != RemoteTaskRunStatus.ACTIVE
                && registration.getStatus() != RemoteTaskRunStatus.PAUSED) {
            return RemoteRunCorrelation.TASK_RUN_MISMATCH;
        }
        if (registration.getStopEpoch() != command.getStop().getStopEpoch()) {
            return RemoteRunCorrelation.TASK_RUN_MISMATCH;
        }
        if (command.getObservationMode() == RemoteObservationMode.PAUSED_READ_ONLY) {
            /*
             * Paused read-only observation: WINDOW_FACT/CAPTURE proceed as ACTIVE-equivalent
             * reads ONLY while the registration is PAUSED; the pre-side-effect revision gate in
             * requireRegistration still enforces command.runRevision == the exact PAUSED
             * registration revision. A marker on an ACTIVE run, or on an input bundle (already
             * rejected by strict schema), fails closed. Normal unmarked commands keep the
             * existing PAUSED -> TASK_RUN_PAUSED mapping below, unchanged.
             */
            if (isPhysicalInputOperation(command.getOperation())) {
                return RemoteRunCorrelation.TASK_RUN_MISMATCH;
            }
            return registration.getStatus() == RemoteTaskRunStatus.PAUSED
                    ? RemoteRunCorrelation.ACTIVE
                    : RemoteRunCorrelation.TASK_RUN_MISMATCH;
        }
        return registration.getStatus() == RemoteTaskRunStatus.PAUSED
                ? RemoteRunCorrelation.PAUSED
                : RemoteRunCorrelation.ACTIVE;
    }

    private static boolean isPhysicalInputOperation(RemoteGameOperation operation) {
        return operation == RemoteGameOperation.EXECUTE_INPUT_BUNDLE
                || operation == RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL
                || operation == RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS
                || operation == RemoteGameOperation.LOCAL_MACRO;
    }

    private static RemoteObservedWindowBinding observedWindow(
            WindowRuntimeContext context,
            WindowNativeBinding binding) {
        return RemoteObservedWindowBinding.builder()
                .windowId(context.getWindowId())
                .nativeHandle(binding.getNativeHandle())
                .processId(binding.getProcessId())
                .playerIdentityEpoch(context.getPlayerIdentityEpoch())
                .build();
    }

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private record BindingAccess(
            WindowTaskRunner runner,
            WindowRuntimeContext context,
            WindowNativeBinding binding) {
    }

    private record ClientInputGeometry(int x, int y, int width, int height) {
        private static ClientInputGeometry from(WindowNativeBinding binding) {
            return new ClientInputGeometry(
                    binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight());
        }

        private boolean matches(WindowTaskRunner runner) {
            WindowRuntimeContext context = runner == null ? null : runner.getWindowContext();
            WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
            return hasGeometry()
                    && binding != null
                    && binding.hasGeometry()
                    && x == binding.getX()
                    && y == binding.getY()
                    && width == binding.getWidth()
                    && height == binding.getHeight();
        }

        private boolean hasGeometry() {
            return width > 0 && height > 0;
        }
    }

    private record CaptureRectangle(int x1, int y1, int x2, int y2, int width, int height) {
    }

    private enum RemoteRunCorrelation {
        CORRELATED,
        ACTIVE,
        PAUSED,
        STOP_REQUESTED,
        TASK_RUN_MISMATCH,
        WINDOW_BINDING_CHANGED
    }

    private record OperationTiming(long acceptedAtEpochMs, long startedAtNanos, long timeoutNanos) {
        private static OperationTiming start(long timeoutMs) {
            return new OperationTiming(
                    System.currentTimeMillis(),
                    System.nanoTime(),
                    TimeUnit.MILLISECONDS.toNanos(Math.max(1L, timeoutMs)));
        }

        private boolean timedOut() {
            return System.nanoTime() - startedAtNanos >= timeoutNanos;
        }

        private long deadlineNanos() {
            return startedAtNanos + timeoutNanos;
        }
    }

    private static final class TerminalSignal extends RuntimeException {
        private final RemoteExecutionState executionState;
        private final RemoteOutcomeCode code;

        private TerminalSignal(
                RemoteExecutionState executionState,
                RemoteOutcomeCode code,
                String message) {
            super(message);
            this.executionState = executionState;
            this.code = code;
        }
    }
}
