package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnCoreProtocolGoldenJsonTest {

    @Test
    void closedEnumsRemainExact() {
        assertArrayEquals(new String[]{"CAPTURE", "MATCH_TEMPLATE", "INPUT", "WAIT", "LOCAL_SERVICE"},
                names(TurnStepType.values()));
        assertArrayEquals(new String[]{"MOVE_MOUSE", "CLICK_LEFT", "CLICK_RIGHT", "DOUBLE_CLICK_LEFT",
                        "DOUBLE_CLICK_RIGHT", "DRAG_LEFT", "SCROLL", "KEY_TAP", "KEY_DOWN", "KEY_UP",
                        "TEXT_INPUT"},
                names(TurnInputAction.values()));
        assertArrayEquals(new String[]{"BAG_RETURN_ITEM", "BAG_USE_INCENSE", "UI_CLEAN_ALL",
                        "UI_CLOSE_GENERIC_WINDOWS", "UI_CLEAN_LIGHTWEIGHT", "UI_CLOSE_MAP_SEARCH_INPUT_BY_X2",
                        "GIVE_ITEM_FROM_OPEN_DIALOG", "QUEST_ACTIVATE", "QUEST_CAPTURE_DETAIL",
                        "TASK_TRACKER_CAPTURE_PANEL",
                        "WHOLE_TASK_PATHING_REGISTER", "WHOLE_TASK_PATHING_READ", "WHOLE_TASK_PATHING_CLEAR_INTENT",
                        "WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX", "WHOLE_TASK_PATHING_CLEAR",
                        "WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP",
                        "WHOLE_TASK_MOVEMENT_INTENT_RECORD", "WHOLE_TASK_TARGET_MAP_GATE_START",
                        "WHOLE_TASK_TARGET_MAP_GATE_OPEN", "WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST",
                        "WHOLE_TASK_PRE_BATTLE_TIMER_READ", "WHOLE_TASK_PRE_BATTLE_FACT_READ",
                        "WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK", "WHOLE_TASK_PRE_BATTLE_TIMER_START",
                        "WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE", "WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR",
                        "WHOLE_TASK_DIALOG_INTEREST_UPDATE", "WHOLE_TASK_DIALOG_INTEREST_CLEAR",
                        "WHOLE_TASK_PROGRESS_UPDATE", "WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME",
                        "WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE",
                        "WHOLE_TASK_DIALOG_RUNTIME_READ", "WHOLE_TASK_COMBAT_ENTRY_CLEANUP",
                        "WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE",
                        "WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME",
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ",
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE",
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME",
                        "METRIC_RECORD_ROUND_STARTED", "METRIC_RECORD_ROUND_FINISHED",
                        "METRIC_RECORD_XIULUO_FAILURE_CASE",
                        "BAG_FIVERING_SUPPLY_CHECK", "BAG_FIND_AND_USE_FROM_BACK",
                        "BAG_FIND_ITEM_PAGE_INDEX", "HOST_SLEEP_COMPUTER", "MAP_SURVEY_POINTER_SAMPLE"},
                names(TurnLocalOperation.values()));
        assertArrayEquals(new String[]{"CAPTURE", "MATCH_EVIDENCE", "QUEST_DETAIL", "TASK_TRACKER_PANEL",
                        "FIVERING_INCENSE_OBSERVATION", "FAILURE_EVIDENCE"},
                names(TurnFramePurpose.values()));
        assertArrayEquals(new String[]{"UPLOAD_IMAGE", "NO_IMAGE"},
                names(TurnCaptureSpec.ResultMode.values()));
        assertArrayEquals(new String[]{"NONE", "CLICK"}, names(TurnMatchSpec.OnMatch.values()));
        assertArrayEquals(new String[]{"RETURN_MATCH_RESULT", "RETURN_MATCH_RESULT_AND_IMAGE"},
                names(TurnMatchSpec.ResultMode.values()));
        assertArrayEquals(new String[]{"COMPLETED", "FAILED", "STOPPED", "DUPLICATE_OR_UNCERTAIN"},
                names(TurnOutcome.Status.values()));
        assertArrayEquals(new String[]{"COMPLETED", "FAILED", "NOT_RUN"},
                names(TurnStepResult.Status.values()));
        assertArrayEquals(new String[]{"ACTION", "IDLE", "CONTINUATION"}, names(TurnResponse.Status.values()));
        assertArrayEquals(new String[]{"WUHUAN_V2", "WUBEI", "XIULUO_V2", "XINSHOU", "WILD_BATTLE",
                        "TIANTING", "AUTO_BATTLE", "SLEEP_COMPUTER"},
                names(TurnTaskCode.values()));
        assertArrayEquals(new String[]{"CONTINUE_ON_FAILURE", "STOP_ON_FAILURE"},
                names(TurnTaskQueueFailurePolicy.values()));
        assertArrayEquals(
                new String[]{"PRESCAN_TASK_PAGE", "PRESCAN_FROM_BACK", "USE_CACHED_RETURN_ITEM",
                        "FIND_AND_USE_TASK_PAGE"},
                names(TurnBagOperationArguments.ReturnItemIntent.values()));
    }

    @Test
    void allFiveStepKindsAndElevenInputActionsRoundTripThroughStrictContractMapper() throws IOException {
        List<TurnInputAction> inputActions = List.of(TurnInputAction.values());
        List<TurnInputSpec> inputSpecs = List.of(
                new TurnInputSpec(100, 200, null, null, null, null, null),
                new TurnInputSpec(101, 201, null, null, null, null, null),
                new TurnInputSpec(102, 202, null, null, null, null, null),
                new TurnInputSpec(103, 203, null, null, null, null, null),
                new TurnInputSpec(104, 204, null, null, null, null, null),
                new TurnInputSpec(105, 205, 305, 405, null, null, null),
                new TurnInputSpec(106, 206, null, null, -3, null, null),
                new TurnInputSpec(null, null, null, null, null, "ALT_E", null),
                new TurnInputSpec(null, null, null, null, null, "CTRL", null),
                new TurnInputSpec(null, null, null, null, null, "CTRL", null),
                new TurnInputSpec(null, null, null, null, null, null, "Changan"));

        List<TurnStep> steps = new ArrayList<>();
        for (int index = 0; index < inputActions.size(); index++) {
            steps.add(TurnProtocolGoldenSupport.inputStep(index, inputActions.get(index), inputSpecs.get(index)));
        }
        steps.add(TurnProtocolGoldenSupport.waitStep(11, 800L));
        steps.add(TurnProtocolGoldenSupport.captureStep(12,
                new TurnCaptureSpec(new TurnRegion(20, 30, 640, 480), TurnCaptureSpec.ResultMode.NO_IMAGE)));
        steps.add(TurnProtocolGoldenSupport.matchStep(13,
                new TurnMatchSpec(new TurnRegion(25, 35, 80, 40), "dialog/example", TurnProtocolGoldenSupport.SHA_A,
                        0.92D, TurnMatchSpec.OnMatch.NONE, TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT)));
        steps.add(TurnProtocolGoldenSupport.localStep(14,
                new TurnLocalServiceCall(TurnLocalOperation.BAG_USE_INCENSE, null, null, null, null)));

        TurnAction action = TurnProtocolGoldenSupport.action("core-all-step-kinds", steps);
        assertSame(action, TurnProtocolValidator.requireValid(action));
        assertEquals(EnumSet.allOf(TurnStepType.class), action.steps().stream()
                .map(TurnStep::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TurnStepType.class))));
        assertEquals(inputActions, action.steps().subList(0, 11).stream().map(TurnStep::inputAction).toList());

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);
        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
    }

    @Test
    void pointerClearRetainsSignedScreenCoordinatesWithoutAnyScaleField() throws IOException {
        TurnCaptureSpec.ClearPointerIfOverRegion clear =
                new TurnCaptureSpec.ClearPointerIfOverRegion(128, -700, 400, 5_000);
        TurnAction action = TurnProtocolGoldenSupport.action(
                "core-pointer-clear-negative-monitor",
                List.of(TurnProtocolGoldenSupport.captureStep(
                        0,
                        new TurnCaptureSpec(
                                new TurnRegion(-1_500, -200, 100, 80),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                                clear))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        TurnProtocolValidator.requireValid(roundTripped);
        TurnCaptureSpec.ClearPointerIfOverRegion actual =
                roundTripped.steps().get(0).capture().clearPointerIfOverRegion();
        assertEquals(128, actual.paddingPx());
        assertEquals(-700, actual.targetX());
        assertEquals(400, actual.targetY());
        assertEquals(5_000, actual.settleMs());
        JsonNode clearJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER
                .valueToTree(roundTripped)
                .path("steps")
                .path(0)
                .path("capture")
                .path("clearPointerIfOverRegion");
        assertFalse(clearJson.has("systemScaleRatio"));
    }

    @Test
    void allLocalOperationsRetainTheirTypedArgumentUnion() throws IOException {
        List<TurnLocalServiceCall> calls = List.of(
                new TurnLocalServiceCall(TurnLocalOperation.BAG_RETURN_ITEM,
                        new TurnBagOperationArguments(TurnBagOperationArguments.ReturnItemIntent.USE_CACHED_RETURN_ITEM,
                                null, -1, new TurnReturnItemCachePoint(
                                "items/return.png", 1320, 760, 123_456_789L, "golden"), "golden"),
                        null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.BAG_USE_INCENSE, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLEAN_ALL, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLOSE_GENERIC_WINDOWS, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT, null,
                        new TurnUiOperationArguments("golden"), null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2, null,
                        new TurnUiOperationArguments("golden"), null, null),
                new TurnLocalServiceCall(TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG, null, null,
                        new TurnGiveItemOperationArguments("items/shoe.png", 2), null),
                new TurnLocalServiceCall(TurnLocalOperation.QUEST_ACTIVATE, null, null, null,
                        new TurnQuestOperationArguments("wuhuan", Boolean.TRUE)),
                new TurnLocalServiceCall(TurnLocalOperation.QUEST_CAPTURE_DETAIL, null, null, null,
                        new TurnQuestOperationArguments("wuhuan", null)),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_REGISTER, wtBuilder("golden")
                        .pathingIntent(new TurnPathingIntent("golden", "intent-golden", "长安", 1, 2, 5, "TARGETED"))),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_READ, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_INTENT,
                        wtBuilder("golden").intentId("intent-golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX,
                        wtBuilder("golden").sourcePrefix("wubei:tracker-green-click:")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR,
                        wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                        wtBuilder("golden").intentId("intent-golden").targetMapName("宝象国")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_MOVEMENT_INTENT_RECORD,
                        wtBuilder("golden").protectionMs(2000L)),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_START,
                        wtBuilder("golden").taskCode("wubei").targetMapName("宝象国")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_OPEN, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST,
                        wtBuilder("golden").taskCode("wubei")
                                .interestOperations(List.of("WUBEI_ENTER_BATTLE"))),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_READ, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_FACT_READ, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_START,
                        wtBuilder("golden").taskCode("wubei").targetKeyword("kw")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE,
                        wtBuilder("golden").blockedMs(500L)),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_UPDATE,
                        wtBuilder("golden").taskCode("wubei").interestOperations(List.of("WUBEI_ENTER_BATTLE"))),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_CLEAR, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PROGRESS_UPDATE,
                        wtBuilder("golden").completedRuns(1).totalRuns(5)),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE,
                        wtBuilder("golden").startupFlyingState("FLYING")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_DIALOG_RUNTIME_READ,
                        wtBuilder("golden").dialogSnapshotMaxAgeMs(1500L)),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_COMBAT_ENTRY_CLEANUP,
                        wtBuilder("golden").taskCode("wubei").sourcePrefix("wubei:tracker-green-click")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE,
                        wtBuilder("golden").transferChoice(new TurnPendingTransferChoice(
                                "长安", 1320, 760, "宝象国", 12, -8, "前往宝象国", "golden", 123_456_789L))),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                        wtBuilder("golden").intentId("intent-golden").sourcePrefix("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ, wtBuilder("golden")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                        wtBuilder("golden")
                                .routeOutcome(new TurnPendingRouteOutcome(
                                        "长安", "宝象国", "YELLOW_DESTINATION_MINI_MAP", 12, -8, "宝象国", "golden",
                                        true, "route-golden", "intent-golden", 123_456_789L))
                                .routeOutcomeReplacementReason("golden-replacement")),
                wholeTaskCall(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME,
                        wtBuilder("golden").intentId("intent-golden").sourcePrefix("golden")),
                metricCall(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                        "wubei", "五倍", "window-golden", "LEADER", "0x5150",
                        "round-7", 7, "普通怪", null, null, "轮次开始", null,
                        null, null, null, null, Map.of("sourcePhase", "ACCEPT"))),
                metricCall(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, new TurnMetricEventPayload(
                        "wubei", "五倍", "window-golden", "LEADER", "0x5150",
                        "round-7", 7, "普通怪", "SUCCESS", "SUCCESS", "轮次完成", 1234L,
                        null, null, null, null, Map.of("sourcePhase", "COMBAT"))),
                metricCall(TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE, new TurnMetricEventPayload(
                        "xiuluo_v2", "修罗", "window-golden", "MEMBER", "0x5151",
                        null, null, null, null, null, "watchdog timeout", null,
                        "D:\\cloud\\cases\\2026-07-18\\case-golden", "PRE_COMBAT_TIMEOUT",
                        "WAIT_TRACKER", 8, null)),
                bagCall(TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK, new TurnBagOperationArguments(
                        null, "wuhuan/shoe.png", 3, null, "golden")),
                bagCall(TurnLocalOperation.BAG_FIND_AND_USE_FROM_BACK, new TurnBagOperationArguments(
                        null, "bag/probe.png", 5, null, "golden")),
                bagCall(TurnLocalOperation.BAG_FIND_ITEM_PAGE_INDEX, new TurnBagOperationArguments(
                        null, "bag/shoe.png", null, null, "golden")),
                new TurnLocalServiceCall(TurnLocalOperation.HOST_SLEEP_COMPUTER,
                        null, null, null, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.MAP_SURVEY_POINTER_SAMPLE,
                        null, null, null, null, null, null, null));

        List<TurnStep> steps = new ArrayList<>();
        for (int index = 0; index < calls.size(); index++) {
            steps.add(TurnProtocolGoldenSupport.localStep(index, calls.get(index)));
        }
        TurnAction action = TurnProtocolGoldenSupport.action("core-all-local-operations", steps);
        TurnProtocolValidator.requireValid(action);
        EnumSet<TurnLocalOperation> covered = action.steps().stream()
                .map(step -> step.localService().operation())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TurnLocalOperation.class)));
        TurnAction trackerAction = TurnProtocolGoldenSupport.action("core-task-tracker", List.of(
                TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                        TurnLocalOperation.TASK_TRACKER_CAPTURE_PANEL,
                        null, null, null, null, null, null,
                        new TurnTaskTrackerOperationArguments("golden")))));
        TurnProtocolValidator.requireValid(trackerAction);
        covered.add(TurnLocalOperation.TASK_TRACKER_CAPTURE_PANEL);
        assertEquals(EnumSet.allOf(TurnLocalOperation.class), covered);
        assertEquals(action, TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class));
    }

    private static TurnLocalServiceCall wholeTaskCall(TurnLocalOperation operation, WtArgs args) {
        return new TurnLocalServiceCall(operation, null, null, null, null, args.build());
    }

    private static TurnLocalServiceCall metricCall(TurnLocalOperation operation, TurnMetricEventPayload payload) {
        return new TurnLocalServiceCall(operation, null, null, null, null, null, payload);
    }

    private static TurnLocalServiceCall bagCall(TurnLocalOperation operation, TurnBagOperationArguments bag) {
        return new TurnLocalServiceCall(operation, bag, null, null, null);
    }

    private static WtArgs wtBuilder(String source) {
        return new WtArgs(source);
    }

    /** Compact builder mirroring the wire record so golden calls stay readable. */
    private static final class WtArgs {
        private final String source;
        private TurnPathingIntent pathingIntent;
        private String intentId;
        private String sourcePrefix;
        private Long protectionMs;
        private Integer currentX;
        private Integer currentY;
        private String targetMapName;
        private Integer targetX;
        private Integer targetY;
        private Integer tolerance;
        private Long confirmTimeoutMs;
        private String taskCode;
        private String targetKeyword;
        private Long blockedMs;
        private List<String> interestOperations;
        private Integer completedRuns;
        private Integer totalRuns;
        private Long dialogSnapshotMaxAgeMs;
        private TurnPendingTransferChoice transferChoice;
        private TurnPendingRouteOutcome routeOutcome;
        private String routeOutcomeReplacementReason;
        private String startupFlyingState;
        private String recoveryTaskRunId;
        private Integer recoveryRound;
        private String recoveryAttemptId;

        private WtArgs(String source) {
            this.source = source;
        }

        private WtArgs pathingIntent(TurnPathingIntent v) { this.pathingIntent = v; return this; }
        private WtArgs intentId(String v) { this.intentId = v; return this; }
        private WtArgs sourcePrefix(String v) { this.sourcePrefix = v; return this; }
        private WtArgs protectionMs(Long v) { this.protectionMs = v; return this; }
        private WtArgs currentX(Integer v) { this.currentX = v; return this; }
        private WtArgs currentY(Integer v) { this.currentY = v; return this; }
        private WtArgs targetMapName(String v) { this.targetMapName = v; return this; }
        private WtArgs targetX(Integer v) { this.targetX = v; return this; }
        private WtArgs targetY(Integer v) { this.targetY = v; return this; }
        private WtArgs tolerance(Integer v) { this.tolerance = v; return this; }
        private WtArgs confirmTimeoutMs(Long v) { this.confirmTimeoutMs = v; return this; }
        private WtArgs taskCode(String v) { this.taskCode = v; return this; }
        private WtArgs targetKeyword(String v) { this.targetKeyword = v; return this; }
        private WtArgs blockedMs(Long v) { this.blockedMs = v; return this; }
        private WtArgs interestOperations(List<String> v) { this.interestOperations = v; return this; }
        private WtArgs completedRuns(Integer v) { this.completedRuns = v; return this; }
        private WtArgs totalRuns(Integer v) { this.totalRuns = v; return this; }
        private WtArgs dialogSnapshotMaxAgeMs(Long v) { this.dialogSnapshotMaxAgeMs = v; return this; }
        private WtArgs transferChoice(TurnPendingTransferChoice v) { this.transferChoice = v; return this; }
        private WtArgs routeOutcome(TurnPendingRouteOutcome v) { this.routeOutcome = v; return this; }
        private WtArgs routeOutcomeReplacementReason(String v) { this.routeOutcomeReplacementReason = v; return this; }
        private WtArgs startupFlyingState(String v) { this.startupFlyingState = v; return this; }
        private WtArgs recoveryIdentity(String taskRunId, int round, String attemptId) {
            this.recoveryTaskRunId = taskRunId;
            this.recoveryRound = round;
            this.recoveryAttemptId = attemptId;
            return this;
        }

        private TurnWholeTaskRuntimeArguments build() {
            return new TurnWholeTaskRuntimeArguments(
                    source, pathingIntent, intentId, sourcePrefix, protectionMs, null, currentX, currentY,
                    targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword,
                    blockedMs, interestOperations, null, null, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                    transferChoice, routeOutcome, routeOutcomeReplacementReason, startupFlyingState,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, recoveryTaskRunId, recoveryRound, recoveryAttemptId);
        }
    }

    @Test
    void nonZeroWindowOriginAndPauseStopFlagsRoundTripExactly() throws IOException {
        TurnWindowMetadata window = TurnProtocolGoldenSupport.window(true, false);
        TurnRequest request = new TurnRequest(1, window, 25_000L, null, null);

        TurnRequest roundTripped = TurnProtocolGoldenSupport.roundTrip(request, TurnRequest.class);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(120, roundTripped.window().windowRect().left());
        assertEquals(80, roundTripped.window().windowRect().top());
        assertEquals(1280, roundTripped.window().windowRect().width());
        assertEquals(720, roundTripped.window().windowRect().height());
        assertEquals(true, roundTripped.window().pauseRequested());
        assertFalse(roundTripped.window().stopRequested());
    }

    @Test
    void lifecycleTaskCodeIncludesExplicitSleepComputerHostTask() {
        assertEquals(TurnTaskCode.SLEEP_COMPUTER, TurnTaskCode.valueOf("SLEEP_COMPUTER"));
    }

    @Test
    void findAndUseTaskPageReturnItemRoundTripsThroughStrictContractMapper() throws IOException {
        TurnLocalServiceCall call = new TurnLocalServiceCall(
                TurnLocalOperation.BAG_RETURN_ITEM,
                new TurnBagOperationArguments(
                        TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                        "bag/xiuluo_return_item.png", -1, null, "golden"),
                null, null, null);
        TurnAction action = TurnProtocolGoldenSupport.action(
                "core-find-and-use-task-page",
                List.of(TurnProtocolGoldenSupport.localStep(0, call)));

        assertSame(action, TurnProtocolValidator.requireValid(action));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);
        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);

        // P-PROTO result carrier (folded here to keep the frozen golden count): the nullable
        // pendingRouteOutcome and its five-field backward-compatible constructor round-trip through the
        // strict contract mapper, and the compat constructor defaults pendingRouteOutcome to null.
        TurnPendingRouteOutcome routeOutcome = new TurnPendingRouteOutcome(
                "长安", "宝象国", "YELLOW_DESTINATION_MINI_MAP", 12, -8, "宝象国", "golden",
                true, "route-golden", "intent-golden", 123_456_789L);
        TurnWholeTaskRuntimeResult present =
                new TurnWholeTaskRuntimeResult(null, null, null, null, null, routeOutcome);
        assertEquals(present, TurnProtocolGoldenSupport.roundTrip(present, TurnWholeTaskRuntimeResult.class));
        assertEquals(routeOutcome, TurnProtocolGoldenSupport.roundTrip(
                present, TurnWholeTaskRuntimeResult.class).pendingRouteOutcome());
        TurnWholeTaskRuntimeResult compat =
                new TurnWholeTaskRuntimeResult(Boolean.TRUE, "EXECUTED", 1_500L, null, null);
        assertEquals(new TurnWholeTaskRuntimeResult(Boolean.TRUE, "EXECUTED", 1_500L, null, null, null), compat);
        assertEquals(compat, TurnProtocolGoldenSupport.roundTrip(compat, TurnWholeTaskRuntimeResult.class));
    }

    @Test
    void exactAttemptRecoveryResetArgumentsAndAckRoundTripStrictly() throws IOException {
        TurnWholeTaskRuntimeArguments arguments = wtBuilder("g010-reset")
                .recoveryIdentity("task-run-10", 4, "attempt-10-4").build();
        TurnAction action = TurnProtocolGoldenSupport.action(
                "g010-exact-reset",
                List.of(TurnProtocolGoldenSupport.localStep(0,
                        new TurnLocalServiceCall(
                                TurnLocalOperation.WHOLE_TASK_RECOVERY_RESET,
                                null, null, null, null, arguments))));
        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);
        assertEquals(action, roundTripped);
        assertSame(roundTripped, TurnProtocolValidator.requireValid(roundTripped));

        TurnExactAttemptRecoveryResetAck ack = new TurnExactAttemptRecoveryResetAck(
                "task-run-10", 4, "attempt-10-4", true,
                true, true, true, true, true, true, true, true, true, false);
        TurnWholeTaskRuntimeResult result = new TurnWholeTaskRuntimeResult(
                null, null, null, null, null, null, null, null, null, null, ack);
        assertEquals(result, TurnProtocolGoldenSupport.roundTrip(result, TurnWholeTaskRuntimeResult.class));
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}

final class TurnProtocolGoldenSupport {

    static final String SHA_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    static final String SHA_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    static final ObjectMapper STRICT_CONTRACT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private TurnProtocolGoldenSupport() {
    }

    static TurnWindowMetadata window(boolean pauseRequested, boolean stopRequested) {
        return new TurnWindowMetadata(
                "device-alpha",
                "window-2",
                "Classic Client - Alpha",
                "0x000000000001A2B3",
                4242L,
                new TurnWindowRect(120, 80, 1280, 720),
                pauseRequested,
                stopRequested);
    }

    static TurnAction action(String actionId, List<TurnStep> steps) {
        return new TurnAction(1, actionId, "device-alpha", "window-2", List.copyOf(steps), true);
    }

    static TurnStep inputStep(int index, TurnInputAction action, TurnInputSpec input) {
        return new TurnStep(index, TurnStepType.INPUT, action, input, null, null, null, null);
    }

    static TurnStep waitStep(int index, long waitMs) {
        return new TurnStep(index, TurnStepType.WAIT, null, null, waitMs, null, null, null);
    }

    static TurnStep captureStep(int index, TurnCaptureSpec capture) {
        return new TurnStep(index, TurnStepType.CAPTURE, null, null, null, capture, null, null);
    }

    static TurnStep matchStep(int index, TurnMatchSpec match) {
        return new TurnStep(index, TurnStepType.MATCH_TEMPLATE, null, null, null, null, match, null);
    }

    static TurnStep localStep(int index, TurnLocalServiceCall localService) {
        return new TurnStep(index, TurnStepType.LOCAL_SERVICE, null, null, null, null, null, localService);
    }

    static <T> T readFixture(String name, Class<T> type) throws IOException {
        return STRICT_CONTRACT_MAPPER.readValue(fixtureBytes(name), type);
    }

    static <T> T assertFixtureRoundTrip(String name, Class<T> type) throws IOException {
        byte[] bytes = fixtureBytes(name);
        JsonNode expected = STRICT_CONTRACT_MAPPER.readTree(bytes);
        T value = STRICT_CONTRACT_MAPPER.readValue(bytes, type);
        JsonNode actual = STRICT_CONTRACT_MAPPER.readTree(STRICT_CONTRACT_MAPPER.writeValueAsBytes(value));
        assertEquals(expected, actual, "strict contract mapper changed canonical fixture fields");
        return value;
    }

    static <T> T roundTrip(T value, Class<T> type) throws IOException {
        return STRICT_CONTRACT_MAPPER.readValue(STRICT_CONTRACT_MAPPER.writeValueAsBytes(value), type);
    }

    static byte[] fixtureBytes(String name) throws IOException {
        String resource = "cloud-turn/v1/" + name;
        ClassLoader loader = TurnProtocolGoldenSupport.class.getClassLoader();
        try (InputStream input = Objects.requireNonNull(loader.getResourceAsStream(resource),
                "missing fixture " + resource)) {
            return input.readAllBytes();
        }
    }
}
