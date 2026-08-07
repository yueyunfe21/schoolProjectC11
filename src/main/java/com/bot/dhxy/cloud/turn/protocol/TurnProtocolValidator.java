package com.bot.dhxy.cloud.turn.protocol;

import com.bot.dhxy.runner.context.TaskStartupMode;

import java.util.EnumSet;
import java.util.List;

/**
 * Validates the closed shared HTTPS turn protocol before it crosses a process boundary.
 */
public final class TurnProtocolValidator {

    private static final int CONTRACT_VERSION = 1;

    private TurnProtocolValidator() {
    }

    public static TurnRequest requireValid(TurnRequest request) {
        require(request != null, "request must not be null");
        requireContractVersion(request.contractVersion());
        requireWindow(request.window(), "request.window");
        require(request.waitTimeoutMs() >= 0L, "request.waitTimeoutMs must not be negative");
        if (request.previousOutcome() != null) {
            TurnOutcome outcome = requireValid(request.previousOutcome());
            requireSameWindow(request.window(), outcome.window(), "request.previousOutcome.window");
        }
        if (request.taskStartRequest() != null) {
            requireTaskStartRequest(request.taskStartRequest());
            requireTaskStartWindowAuthority(request.window());
        }
        if (request.mapSurveyCommand() != null) {
            require(request.taskStartRequest() == null && request.continuation() == null,
                    "mapSurveyCommand must not contain taskStartRequest or continuation");
            require(request.mapSurveyResultAckId() == null,
                    "mapSurveyCommand must not contain mapSurveyResultAckId");
            requireMapSurveyCommand(request.mapSurveyCommand());
        }
        if (request.mapSurveyResultAckId() != null) {
            requireText(request.mapSurveyResultAckId(), "request.mapSurveyResultAckId");
            require(request.taskStartRequest() == null && request.continuation() == null,
                    "mapSurveyResultAckId must not contain taskStartRequest or continuation");
        }
        if (request.continuation() != null) {
            require(request.previousOutcome() == null,
                    "continuation request must not contain previousOutcome");
            require(request.taskStartRequest() == null,
                    "continuation request must not contain taskStartRequest");
            require(request.waitTimeoutMs() == 0L,
                    "continuation request waitTimeoutMs must be zero");
            requireContinuationRequest(request.continuation());
        }
        return request;
    }

    public static TurnResponse requireValid(TurnResponse response) {
        require(response != null, "response must not be null");
        require(response.status() != null, "response.status must not be null");
        switch (response.status()) {
            case ACTION -> {
                require(response.action() != null, "ACTION response requires action");
                require(response.continuationDecision() == null,
                        "ACTION response must not contain continuationDecision");
            }
            case IDLE -> {
                require(response.action() == null, "IDLE response must not contain action");
                require(response.continuationDecision() == null,
                        "IDLE response must not contain continuationDecision");
            }
            case CONTINUATION -> {
                require(response.action() == null, "CONTINUATION response must not contain action");
                require(response.taskStartAck() == null,
                        "CONTINUATION response must not contain taskStartAck");
                requireContinuationDecision(response.continuationDecision());
            }
        }
        if (response.action() != null) {
            requireValid(response.action());
        }
        if (response.taskStartAck() != null) {
            requireTaskStartAck(response.taskStartAck());
        }
        if (response.mapSurveyResult() != null) {
            requireMapSurveyResult(response.mapSurveyResult());
            require(response.status() != TurnResponse.Status.CONTINUATION,
                    "CONTINUATION response must not contain mapSurveyResult");
        }
        if (response.taskTerminalResult() != null) {
            requireText(response.taskTerminalResult().startRequestId(),
                    "taskTerminalResult.startRequestId");
            require(response.taskTerminalResult().status() != null,
                    "taskTerminalResult.status must not be null");
            require(response.status() != TurnResponse.Status.CONTINUATION,
                    "CONTINUATION response must not contain taskTerminalResult");
        }
        for (TurnTaskQueueEvent event : response.taskQueueEvents()) {
            require(event != null, "taskQueueEvents must not contain null");
            requireText(event.eventId(), "taskQueueEvent.eventId");
            requireText(event.startRequestId(), "taskQueueEvent.startRequestId");
            requireText(event.taskRunId(), "taskQueueEvent.taskRunId");
            requireText(event.taskCode(), "taskQueueEvent.taskCode");
            requireText(event.taskName(), "taskQueueEvent.taskName");
            require(event.queueIndex() >= 0, "taskQueueEvent.queueIndex must not be negative");
            require(event.type() != null, "taskQueueEvent.type must not be null");
            require(event.elapsedMs() >= 0L, "taskQueueEvent.elapsedMs must not be negative");
            require(response.status() != TurnResponse.Status.CONTINUATION,
                    "CONTINUATION response must not contain taskQueueEvents");
        }
        return response;
    }

    public static TurnResponse requireValid(TurnResponse response, TurnRequest request) {
        TurnRequest validRequest = requireValid(request);
        TurnResponse validResponse = requireValid(response);
        if (validRequest.continuation() != null) {
            require(validResponse.status() == TurnResponse.Status.CONTINUATION,
                    "continuation request requires CONTINUATION response");
            return validResponse;
        }
        require(validResponse.status() != TurnResponse.Status.CONTINUATION,
                "normal turn request must not receive CONTINUATION response");
        if (validResponse.action() != null) {
            require(validRequest.window().deviceId().equals(validResponse.action().deviceId())
                            && validRequest.window().windowId().equals(validResponse.action().windowId()),
                    "response.action must match request deviceId and windowId");
        }
        if (validRequest.taskStartRequest() == null) {
            require(validResponse.taskStartAck() == null,
                    "response must not contain taskStartAck without taskStartRequest");
        } else {
            require(validResponse.taskStartAck() != null,
                    "response requires taskStartAck for taskStartRequest");
            require(validRequest.taskStartRequest().startRequestId()
                            .equals(validResponse.taskStartAck().startRequestId()),
                    "taskStartAck.startRequestId must match taskStartRequest.startRequestId");
        }
        if (validRequest.mapSurveyCommand() == null) {
            require(validResponse.mapSurveyResult() == null,
                    "response must not contain mapSurveyResult without mapSurveyCommand");
        } else {
            require(validResponse.mapSurveyResult() != null,
                    "response requires mapSurveyResult for mapSurveyCommand");
            require(validRequest.mapSurveyCommand().commandId()
                            .equals(validResponse.mapSurveyResult().commandId()),
                    "mapSurveyResult.commandId must match mapSurveyCommand.commandId");
        }
        return validResponse;
    }

    private static void requireMapSurveyCommand(TurnMapSurveyCommand command) {
        requireText(command.commandId(), "mapSurveyCommand.commandId");
        require(command.operation() != null, "mapSurveyCommand.operation must not be null");
        if (command.mapName() != null) {
            requireText(command.mapName(), "mapSurveyCommand.mapName");
        }
    }

    private static void requireMapSurveyResult(TurnMapSurveyResult result) {
        requireText(result.commandId(), "mapSurveyResult.commandId");
        require(result.status() != null, "mapSurveyResult.status must not be null");
        requireText(result.message(), "mapSurveyResult.message");
        require((result.projectedX() == null) == (result.projectedY() == null),
                "mapSurveyResult projected coordinates must be both present or both absent");
    }

    private static void requireContinuationRequest(TurnContinuationRequest continuation) {
        requireText(continuation.actionId(), "continuation.actionId");
        require(continuation.sourceStepIndex() >= 0,
                "continuation.sourceStepIndex must be nonnegative");
        require(continuation.kind() != null, "continuation.kind must not be null");
        require(continuation.stage() != null, "continuation.stage must not be null");
        boolean kindStageAllowed = switch (continuation.kind()) {
            case FIVERING_INCENSE -> continuation.stage() == TurnContinuationRequest.Stage.TICK
                    || continuation.stage() == TurnContinuationRequest.Stage.STATUS_IMAGE
                    || continuation.stage() == TurnContinuationRequest.Stage.OUTCOME_USED
                    || continuation.stage() == TurnContinuationRequest.Stage.OUTCOME_NOT_FOUND;
        };
        require(kindStageAllowed,
                continuation.stage() + " is not valid for continuation kind " + continuation.kind());
        boolean incenseImage = continuation.kind() == TurnContinuationRequest.Kind.FIVERING_INCENSE
                && continuation.stage() == TurnContinuationRequest.Stage.STATUS_IMAGE;
        if (incenseImage) {
            require(continuation.frame() != null, continuation.stage() + " continuation requires frame");
            requireFrame(continuation.frame());
            TurnFramePurpose expected = TurnFramePurpose.FIVERING_INCENSE_OBSERVATION;
            require(continuation.frame().purpose() == expected,
                    continuation.stage() + " frame purpose must be " + expected);
            require(Integer.valueOf(continuation.sourceStepIndex()).equals(
                            continuation.frame().sourceStepIndex()),
                    "STATUS_IMAGE frame sourceStepIndex must match continuation");
        } else {
            require(continuation.frame() == null,
                    continuation.stage() + " continuation must not contain frame");
        }
        boolean outcome = continuation.stage() == TurnContinuationRequest.Stage.OUTCOME_USED
                || continuation.stage() == TurnContinuationRequest.Stage.OUTCOME_NOT_FOUND;
        if (outcome) {
            requireText(continuation.decisionId(), "continuation.decisionId");
        } else {
            require(continuation.decisionId() == null,
                    continuation.stage() + " continuation must not contain decisionId");
        }
    }

    private static void requireContinuationDecision(TurnContinuationDecision decision) {
        require(decision != null, "CONTINUATION response requires continuationDecision");
        require(decision.directive() != null, "continuationDecision.directive must not be null");
        requireText(decision.reason(), "continuationDecision.reason");
        boolean directedAction = decision.directive() == TurnContinuationDecision.Directive.USE_INCENSE
                || decision.directive() == TurnContinuationDecision.Directive.CLOSE_STORY;
        if (directedAction) {
            requireText(decision.decisionId(), decision.directive() + " continuationDecision.decisionId");
        } else {
            require(decision.decisionId() == null,
                    decision.directive() + " continuationDecision must not contain decisionId");
        }
        require(decision.clickX() == null && decision.clickY() == null,
                decision.directive() + " continuationDecision must not contain click coordinates");
    }

    public static TurnAction requireValid(TurnAction action) {
        require(action != null, "action must not be null");
        requireContractVersion(action.contractVersion());
        requireText(action.actionId(), "action.actionId");
        requireText(action.deviceId(), "action.deviceId");
        requireText(action.windowId(), "action.windowId");
        require(action.steps() != null && !action.steps().isEmpty(), "action.steps must not be empty");

        int uploadFrameCount = 0;
        boolean pixelChangeProbe = false;
        for (int index = 0; index < action.steps().size(); index++) {
            TurnStep step = action.steps().get(index);
            require(step != null, "action.steps[" + index + "] must not be null");
            require(step.index() == index, "step.index must equal its list index");
            uploadFrameCount += requireValidStep(step, index);
            pixelChangeProbe |= step.type() == TurnStepType.CAPTURE
                    && step.capture().pixelChangeProbe() != null;
        }
        require(uploadFrameCount <= 1, "action may request at most one returned frame");
        if (pixelChangeProbe) {
            require(action.steps().size() == 1
                            && action.steps().get(0).index() == 0
                            && action.steps().get(0).type() == TurnStepType.CAPTURE,
                    "pixel-change probe action requires only CAPTURE step index 0");
        }
        if (action.pathingIntent() != null) {
            requireText(action.pathingIntent().source(), "action.pathingIntent.source");
            requireText(action.pathingIntent().intentId(), "action.pathingIntent.intentId");
            require(action.pathingIntent().tolerance() >= 0,
                    "action.pathingIntent.tolerance must not be negative");
        }
        return action;
    }

    public static TurnOutcome requireValid(TurnOutcome outcome) {
        require(outcome != null, "outcome must not be null");
        requireContractVersion(outcome.contractVersion());
        requireText(outcome.actionId(), "outcome.actionId");
        requireWindow(outcome.window(), "outcome.window");
        require(outcome.status() != null, "outcome.status must not be null");
        require(outcome.stepResults() != null, "outcome.stepResults must not be null");

        for (int index = 0; index < outcome.stepResults().size(); index++) {
            TurnStepResult result = outcome.stepResults().get(index);
            require(result != null, "outcome.stepResults[" + index + "] must not be null");
            require(result.index() == index, "stepResult.index must equal its list index");
            require(result.type() != null, "stepResult.type must not be null");
            require(result.status() != null, "stepResult.status must not be null");
            requireText(result.code(), "stepResult.code");
            if (result.type() == TurnStepType.MATCH_TEMPLATE && result.match() != null) {
                requireMatchResult(result.match());
            }
            require(result.type() == TurnStepType.MATCH_TEMPLATE || result.match() == null,
                    "only MATCH_TEMPLATE step results may contain match");
            require(result.type() == TurnStepType.LOCAL_SERVICE || result.localResultJson() == null,
                    "only LOCAL_SERVICE step results may contain localResultJson");
        }
        requireOutcomeFailureShape(outcome);
        if (outcome.frame() != null) {
            requireFrame(outcome.frame());
        }
        return outcome;
    }

    private static int requireValidStep(TurnStep step, int index) {
        require(step.type() != null, "step.type must not be null");
        return switch (step.type()) {
            case CAPTURE -> {
                requireOnly(step, step.capture() != null, "capture");
                requireCapture(step.capture());
                yield step.capture().resultMode() == TurnCaptureSpec.ResultMode.UPLOAD_IMAGE ? 1 : 0;
            }
            case MATCH_TEMPLATE -> {
                requireOnly(step, step.match() != null, "match");
                requireMatch(step.match());
                yield step.match().resultMode() == TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT_AND_IMAGE ? 1 : 0;
            }
            case INPUT -> {
                requireOnly(step, step.inputAction() != null && step.input() != null, "inputAction and input");
                requireInput(step.inputAction(), step.input());
                yield 0;
            }
            case WAIT -> {
                requireOnly(step, step.waitMs() != null, "waitMs");
                require(step.waitMs() > 0L, "waitMs must be positive");
                yield 0;
            }
            case LOCAL_SERVICE -> {
                requireOnly(step, step.localService() != null, "localService");
                yield requireLocalService(step.localService());
            }
        };
    }

    private static void requireOnly(TurnStep step, boolean expectedPresent, String expected) {
        require(expectedPresent, "step " + step.index() + " requires " + expected);
        switch (step.type()) {
            case CAPTURE -> require(step.inputAction() == null && step.input() == null && step.waitMs() == null
                    && step.match() == null && step.localService() == null, "capture step has unexpected fields");
            case MATCH_TEMPLATE -> require(step.inputAction() == null && step.input() == null && step.waitMs() == null
                    && step.capture() == null && step.localService() == null, "match step has unexpected fields");
            case INPUT -> require(step.waitMs() == null && step.capture() == null && step.match() == null
                    && step.localService() == null, "input step has unexpected fields");
            case WAIT -> require(step.inputAction() == null && step.input() == null && step.capture() == null
                    && step.match() == null && step.localService() == null, "wait step has unexpected fields");
            case LOCAL_SERVICE -> require(step.inputAction() == null && step.input() == null && step.waitMs() == null
                    && step.capture() == null && step.match() == null, "local service step has unexpected fields");
        }
    }

    private static void requireInput(TurnInputAction action, TurnInputSpec input) {
        if (input.autoCombatPanelDrag() != null) {
            require(Boolean.TRUE.equals(input.autoCombatPanelDrag()) && action == TurnInputAction.DRAG_LEFT,
                    "autoCombatPanelDrag=true is valid only for DRAG_LEFT");
        }
        if (input.coordinateSpace() != null) {
            require(action == TurnInputAction.MOVE_MOUSE
                            || action == TurnInputAction.CLICK_LEFT
                            || action == TurnInputAction.CLICK_RIGHT
                            || action == TurnInputAction.DOUBLE_CLICK_LEFT
                            || action == TurnInputAction.DOUBLE_CLICK_RIGHT
                            || action == TurnInputAction.DRAG_LEFT
                            || action == TurnInputAction.SCROLL,
                    "coordinateSpace is valid only for mouse input");
        }
        switch (action) {
            case MOVE_MOUSE -> {
                requirePoint(input.x(), input.y(), "input");
                require(input.endX() == null && input.endY() == null && input.scrollDelta() == null
                        && input.key() == null && input.text() == null
                        && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "move input has unexpected fields");
            }
            case CLICK_LEFT, CLICK_RIGHT -> {
                requirePoint(input.x(), input.y(), "input");
                require(input.endX() == null && input.endY() == null && input.scrollDelta() == null
                        && input.key() == null && input.text() == null, "click input has unexpected fields");
                requireClickTiming(input.clickDelayMs(), "input.clickDelayMs");
                requireClickTiming(input.queueHoldMs(), "input.queueHoldMs");
            }
            case DOUBLE_CLICK_LEFT, DOUBLE_CLICK_RIGHT -> {
                requirePoint(input.x(), input.y(), "input");
                require(input.endX() == null && input.endY() == null && input.scrollDelta() == null
                                && input.key() == null && input.text() == null
                                && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "double-click input has unexpected fields");
            }
            case DRAG_LEFT -> {
                requirePoint(input.x(), input.y(), "input");
                requirePoint(input.endX(), input.endY(), "input end");
                require(input.scrollDelta() == null && input.key() == null && input.text() == null
                                && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "drag input has unexpected fields");
            }
            case SCROLL -> {
                requirePoint(input.x(), input.y(), "input");
                require(input.scrollDelta() != null && input.scrollDelta() != 0, "scrollDelta must be nonzero");
                require(input.endX() == null && input.endY() == null && input.key() == null && input.text() == null
                                && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "scroll input has unexpected fields");
            }
            case KEY_TAP, KEY_DOWN, KEY_UP -> {
                requireText(input.key(), "input.key");
                require(input.x() == null && input.y() == null && input.endX() == null && input.endY() == null
                                && input.scrollDelta() == null && input.text() == null
                                && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "key input has unexpected fields");
            }
            case TEXT_INPUT -> {
                requireText(input.text(), "input.text");
                require(input.x() == null && input.y() == null && input.endX() == null && input.endY() == null
                                && input.scrollDelta() == null && input.key() == null
                                && input.clickDelayMs() == null && input.queueHoldMs() == null,
                        "text input has unexpected fields");
            }
        }
    }

    private static void requireCapture(TurnCaptureSpec capture) {
        requireRegion(capture.region(), "capture.region", true);
        require(capture.resultMode() != null, "capture.resultMode must not be null");
        TurnCaptureSpec.ClearPointerIfOverRegion clear = capture.clearPointerIfOverRegion();
        if (clear != null) {
            require(capture.pixelChangeProbe() == null,
                    "capture pointer-clear and pixelChangeProbe are mutually exclusive");
            require(capture.region() != null,
                    "capture.clearPointerIfOverRegion requires a non-null capture.region");
            require(clear.paddingPx() >= 0 && clear.paddingPx() <= 128,
                    "capture.clearPointerIfOverRegion.paddingPx must be in [0, 128]");
            require(clear.settleMs() >= 0 && clear.settleMs() <= 5_000,
                    "capture.clearPointerIfOverRegion.settleMs must be in [0, 5000]");
            require(!isInsidePaddedRegion(clear.targetX(), clear.targetY(), capture.region(), clear.paddingPx()),
                    "capture.clearPointerIfOverRegion target must be outside the padded capture.region");
        }

        TurnCaptureSpec.PixelChangeProbe probe = capture.pixelChangeProbe();
        if (probe != null) {
            require(clear == null, "capture pixelChangeProbe and pointer-clear are mutually exclusive");
            require(capture.region() != null, "capture.pixelChangeProbe requires a non-null capture.region");
            require(capture.resultMode() == TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                    "capture.pixelChangeProbe requires UPLOAD_IMAGE");
            require(probe.ctrlDownSettleMs() >= 0 && probe.ctrlDownSettleMs() <= 5_000,
                    "capture.pixelChangeProbe.ctrlDownSettleMs must be in [0, 5000]");
            require(probe.afterMoveSettleMs() >= 0 && probe.afterMoveSettleMs() <= 5_000,
                    "capture.pixelChangeProbe.afterMoveSettleMs must be in [0, 5000]");
            require(probe.ctrlUpSettleMs() >= 0 && probe.ctrlUpSettleMs() <= 5_000,
                    "capture.pixelChangeProbe.ctrlUpSettleMs must be in [0, 5000]");
            require(Double.isFinite(probe.differenceRatioThreshold())
                            && probe.differenceRatioThreshold() >= 0.0D
                            && probe.differenceRatioThreshold() <= 1.0D,
                    "capture.pixelChangeProbe.differenceRatioThreshold must be in [0.0, 1.0]");
            require(isInsideRegion(probe.targetX(), probe.targetY(), capture.region()),
                    "capture.pixelChangeProbe target must be inside capture.region");
        }
    }

    private static void requireMatch(TurnMatchSpec match) {
        requireRegion(match.region(), "match.region", true);
        requireText(match.templateKey(), "match.templateKey");
        requireSha256(match.contentHash(), "match.contentHash");
        require(Double.isFinite(match.threshold()) && match.threshold() >= 0.0D && match.threshold() <= 1.0D,
                "match.threshold must be in [0.0, 1.0]");
        require(match.onMatch() != null, "match.onMatch must not be null");
        require(match.resultMode() != null, "match.resultMode must not be null");
    }

    private static int requireLocalService(TurnLocalServiceCall call) {
        require(call.operation() != null, "localService.operation must not be null");
        boolean metricOperation = call.operation() == TurnLocalOperation.METRIC_RECORD_ROUND_STARTED
                || call.operation() == TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED
                || call.operation() == TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE;
        require(metricOperation || call.metric() == null,
                "only METRIC_* operations may contain metric arguments");
        require(call.operation() == TurnLocalOperation.TASK_TRACKER_CAPTURE_PANEL
                        || call.taskTracker() == null,
                "only TASK_TRACKER_CAPTURE_PANEL may contain taskTracker arguments");
        require(call.operation() == TurnLocalOperation.XINSHOU_TRACKER_LINK_CHAIN
                        || call.xinshouTrackerChain() == null,
                "only XINSHOU_TRACKER_LINK_CHAIN may contain xinshou tracker-chain arguments");
        require(call.operation() == TurnLocalOperation.XINSHOU_MECHANICAL_ACTION
                        || call.xinshouMechanical() == null,
                "only XINSHOU_MECHANICAL_ACTION may contain xinshou mechanical arguments");
        return switch (call.operation()) {
            case BAG_RETURN_ITEM -> {
                require(call.bag() != null && call.ui() == null && call.giveItem() == null && call.quest() == null,
                        "BAG_RETURN_ITEM requires only bag arguments");
                requireBag(call.bag());
                yield 0;
            }
            case BAG_USE_INCENSE, UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS, UI_PROBE_GENERIC_CLOSE,
                 HOST_SLEEP_COMPUTER,
                    MAP_SURVEY_POINTER_SAMPLE, LEFT_TOP_STATUS_OBSERVE -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.metric() == null && call.taskTracker() == null,
                        call.operation() + " must not contain arguments");
                yield 0;
            }
            case UI_CLEAN_LIGHTWEIGHT, UI_CLOSE_MAP_SEARCH_INPUT_BY_X2 -> {
                require(call.bag() == null && call.ui() != null && call.giveItem() == null && call.quest() == null,
                        call.operation() + " requires only UI arguments");
                requireText(call.ui().source(), "localService.ui.source");
                yield 0;
            }
            case GIVE_ITEM_FROM_OPEN_DIALOG -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() != null && call.quest() == null,
                        "GIVE_ITEM_FROM_OPEN_DIALOG requires only give-item arguments");
                requireText(call.giveItem().targetItemTemplate(), "localService.giveItem.targetItemTemplate");
                yield 0;
            }
            case QUEST_ACTIVATE -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null && call.quest() != null,
                        "QUEST_ACTIVATE requires only quest arguments");
                requireText(call.quest().task(), "localService.quest.task");
                require(call.quest().keepOpen() != null, "QUEST_ACTIVATE.keepOpen must not be null");
                yield 0;
            }
            case QUEST_CAPTURE_DETAIL -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null && call.quest() != null,
                        "QUEST_CAPTURE_DETAIL requires only quest arguments");
                requireText(call.quest().task(), "localService.quest.task");
                require(call.quest().keepOpen() == null, "QUEST_CAPTURE_DETAIL.keepOpen must be null");
                yield 1;
            }
            case TASK_TRACKER_CAPTURE_PANEL -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.metric() == null && call.taskTracker() != null,
                        "TASK_TRACKER_CAPTURE_PANEL requires only taskTracker arguments");
                requireText(call.taskTracker().source(), "localService.taskTracker.source");
                yield 1;
            }
            case XINSHOU_TRACKER_LINK_CHAIN -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.metric() == null && call.taskTracker() == null
                                && call.xinshouDrag() == null && call.xinshouTrackerChain() != null,
                        "XINSHOU_TRACKER_LINK_CHAIN requires only xinshou tracker-chain arguments");
                requireText(call.xinshouTrackerChain().source(), "localService.xinshouTrackerChain.source");
                requireSourceWindowRect(
                        call.xinshouTrackerChain().sourceWindowLeft(),
                        call.xinshouTrackerChain().sourceWindowTop(),
                        call.xinshouTrackerChain().sourceWindowWidth(),
                        call.xinshouTrackerChain().sourceWindowHeight(),
                        "XINSHOU_TRACKER_LINK_CHAIN");
                require(call.xinshouTrackerChain().links().size() == 1,
                        "XINSHOU_TRACKER_LINK_CHAIN requires exactly one parser-selected link");
                for (TurnXinshouTrackerLink link : call.xinshouTrackerChain().links()) {
                    require(link != null,
                            "XINSHOU_TRACKER_LINK_CHAIN link must not be null");
                    requirePointInsideSourceWindow(
                            link.x(), link.y(),
                            call.xinshouTrackerChain().sourceWindowLeft(),
                            call.xinshouTrackerChain().sourceWindowTop(),
                            call.xinshouTrackerChain().sourceWindowWidth(),
                            call.xinshouTrackerChain().sourceWindowHeight(),
                            "XINSHOU_TRACKER_LINK_CHAIN link");
                }
                yield 0;
            }
            case XINSHOU_MECHANICAL_ACTION -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.metric() == null && call.taskTracker() == null
                                && call.xinshouDrag() == null && call.xinshouTrackerChain() == null
                                && call.xinshouMechanical() != null,
                        "XINSHOU_MECHANICAL_ACTION requires only xinshou mechanical arguments");
                requireXinshouMechanical(call.xinshouMechanical());
                yield 0;
            }
            case WHOLE_TASK_PATHING_REGISTER,
                 WHOLE_TASK_PATHING_READ,
                 WHOLE_TASK_PATHING_CLEAR_INTENT,
                 WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX,
                 WHOLE_TASK_PATHING_CLEAR,
                 WHOLE_TASK_RECOVERY_RESET,
                 WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                 WHOLE_TASK_MOVEMENT_INTENT_RECORD,
                 WHOLE_TASK_TARGET_MAP_GATE_START,
                 WHOLE_TASK_TARGET_MAP_GATE_OPEN,
                 WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST,
                 WHOLE_TASK_PRE_BATTLE_TIMER_READ,
                 WHOLE_TASK_PRE_BATTLE_FACT_READ,
                 WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK,
                 WHOLE_TASK_PRE_BATTLE_TIMER_START,
                 WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE,
                 WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR,
                 WHOLE_TASK_DIALOG_INTEREST_UPDATE,
                 WHOLE_TASK_DIALOG_INTEREST_CLEAR,
                 WHOLE_TASK_PROGRESS_UPDATE,
                 WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME,
                 WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE,
                 WHOLE_TASK_DIALOG_RUNTIME_READ,
                 WHOLE_TASK_COMBAT_ENTRY_CLEANUP,
                 WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE,
                 WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME,
                  WHOLE_TASK_RETURN_HOME_REPLAY_ARM,
                  WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM,
                  WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                  XIULUO_ACCEPT_DIALOG_TEMPLATE,
                  JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE,
                  CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE,
                  CATCH_GHOST_CANCEL_DIALOG_TEMPLATE -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() != null,
                        call.operation() + " requires only wholeTaskRuntime arguments");
                requireWholeTaskRuntime(call.operation(), call.wholeTaskRuntime());
                yield 0;
            }
            case XINSHOU_DRAG_SWEEP -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.xinshouDrag() != null,
                        "XINSHOU_DRAG_SWEEP requires only xinshouDrag arguments");
                TurnXinshouDragArguments drag = call.xinshouDrag();
                require(drag.segment() > 0, "XINSHOU_DRAG_SWEEP.segment must be positive");
                require(drag.rightX() >= drag.leftX(), "XINSHOU_DRAG_SWEEP right bound must not precede left");
                require(drag.endY() >= drag.startY(), "XINSHOU_DRAG_SWEEP endY must not precede startY");
                require(drag.rowStepPx() > 0, "XINSHOU_DRAG_SWEEP.rowStepPx must be positive");
                require(drag.progressRoiWidth() > 0 && drag.progressRoiHeight() > 0,
                        "XINSHOU_DRAG_SWEEP progress ROI must be a nonempty region");
                yield 0;
            }
            case XINSHOU_DRAG_RELEASE -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.xinshouDrag() == null,
                        "XINSHOU_DRAG_RELEASE takes no arguments");
                yield 0;
            }
            case BAG_FIVERING_SUPPLY_CHECK -> {
                require(call.bag() != null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null,
                        "BAG_FIVERING_SUPPLY_CHECK requires only bag arguments");
                require(call.bag().intent() == null && call.bag().cachedPoint() == null,
                        "BAG_FIVERING_SUPPLY_CHECK carries no return-item intent or cached point");
                requireText(call.bag().targetItemTemplate(),
                        "BAG_FIVERING_SUPPLY_CHECK.targetItemTemplate");
                require(call.bag().maxBagIndex() != null && call.bag().maxBagIndex() > 0,
                        "BAG_FIVERING_SUPPLY_CHECK.requiredCount (maxBagIndex slot) must be positive");
                requireText(call.bag().source(), "BAG_FIVERING_SUPPLY_CHECK.source");
                yield 0;
            }
            case BAG_FIND_AND_USE_FROM_BACK -> {
                require(call.bag() != null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null,
                        "BAG_FIND_AND_USE_FROM_BACK requires only bag arguments");
                require(call.bag().intent() == null && call.bag().cachedPoint() == null,
                        "BAG_FIND_AND_USE_FROM_BACK carries no return-item intent or cached point");
                requireText(call.bag().targetItemTemplate(),
                        "BAG_FIND_AND_USE_FROM_BACK.targetItemTemplate");
                require(call.bag().maxBagIndex() != null && call.bag().maxBagIndex() > 0,
                        "BAG_FIND_AND_USE_FROM_BACK.maxBagIndex must be positive");
                requireText(call.bag().source(), "BAG_FIND_AND_USE_FROM_BACK.source");
                yield 0;
            }
            case BAG_FIND_ITEM_PAGE_INDEX -> {
                require(call.bag() != null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null,
                        "BAG_FIND_ITEM_PAGE_INDEX requires only bag arguments");
                require(call.bag().intent() == null && call.bag().cachedPoint() == null
                                && call.bag().maxBagIndex() == null,
                        "BAG_FIND_ITEM_PAGE_INDEX carries only the template and source");
                requireText(call.bag().targetItemTemplate(),
                        "BAG_FIND_ITEM_PAGE_INDEX.targetItemTemplate");
                requireText(call.bag().source(), "BAG_FIND_ITEM_PAGE_INDEX.source");
                yield 0;
            }
            case METRIC_RECORD_ROUND_STARTED,
                 METRIC_RECORD_ROUND_FINISHED,
                 METRIC_RECORD_XIULUO_FAILURE_CASE -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null
                                && call.quest() == null && call.wholeTaskRuntime() == null
                                && call.metric() != null,
                        call.operation() + " requires only metric arguments");
                requireMetric(call.operation(), call.metric());
                yield 0;
            }
        };
    }

    private static void requireXinshouMechanical(TurnXinshouMechanicalArguments arguments) {
        require(arguments.action() != null,
                "XINSHOU_MECHANICAL_ACTION.action must not be null");
        switch (arguments.action()) {
            case CLICK_RECOVERY_TEMPLATE -> {
                requireText(arguments.recoveryTemplateName(),
                        "CLICK_RECOVERY_TEMPLATE.recoveryTemplateName");
                require("tiaoguo.png".equals(arguments.recoveryTemplateName())
                                || "quedingguan_.png".equals(arguments.recoveryTemplateName())
                                || "confirm.png".equals(arguments.recoveryTemplateName()),
                        "CLICK_RECOVERY_TEMPLATE.recoveryTemplateName is not allow-listed");
                requireNoPreparedPoint(arguments, "CLICK_RECOVERY_TEMPLATE");
            }
            case CLICK_PREPARED_POINT, CAPTURE_COMBAT -> {
                String operation = arguments.action().name();
                require(arguments.recoveryTemplateName() == null,
                        operation + " must not contain recoveryTemplateName");
                require(arguments.screenX() != null && arguments.screenY() != null,
                        operation + " requires screenX/screenY");
                require(arguments.sourceWindowLeft() != null
                                && arguments.sourceWindowTop() != null
                                && arguments.sourceWindowWidth() != null
                                && arguments.sourceWindowHeight() != null,
                        operation + " requires complete source window rect");
                requireSourceWindowRect(
                        arguments.sourceWindowLeft(),
                        arguments.sourceWindowTop(),
                        arguments.sourceWindowWidth(),
                        arguments.sourceWindowHeight(),
                        operation);
                requirePointInsideSourceWindow(
                        arguments.screenX(), arguments.screenY(),
                        arguments.sourceWindowLeft(), arguments.sourceWindowTop(),
                        arguments.sourceWindowWidth(), arguments.sourceWindowHeight(),
                        operation);
            }
            case CONFIRM_ADOPTION,
                 USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
                 USE_SHELL_AND_BLOW,
                 HAND_IN_MATERIALS,
                 REPAIR_ITEMS_ONCE,
                 CLOSE_REPAIR_WINDOW,
                 USE_LUNHUI_ITEM_AND_START,
                 PRESS_ESCAPE,
                 PRESS_ORDINARY_AUTO_COMBAT,
                 RESTORE_AUTO_COMBAT -> {
                require(arguments.recoveryTemplateName() == null,
                        arguments.action() + " must not contain action-specific arguments");
                requireNoPreparedPoint(arguments, arguments.action().name());
            }
        }
    }

    private static void requireNoPreparedPoint(
            TurnXinshouMechanicalArguments arguments,
            String operation) {
        require(arguments.screenX() == null
                        && arguments.screenY() == null
                        && arguments.sourceWindowLeft() == null
                        && arguments.sourceWindowTop() == null
                        && arguments.sourceWindowWidth() == null
                        && arguments.sourceWindowHeight() == null,
                operation + " must not contain prepared-point geometry");
    }

    private static void requireSourceWindowRect(
            int left,
            int top,
            int width,
            int height,
            String field) {
        require(width > 0 && height > 0, field + " source window size must be positive");
    }

    private static void requirePointInsideSourceWindow(
            int x,
            int y,
            int left,
            int top,
            int width,
            int height,
            String field) {
        long right = (long) left + width;
        long bottom = (long) top + height;
        require(x >= left && y >= top && x < right && y < bottom,
                field + " point must be inside source window rect");
    }

    private static void requireMetric(TurnLocalOperation operation, TurnMetricEventPayload m) {
        // Every persisted identity fact must ride the wire; the local authority never synthesizes it.
        requireText(m.taskCode(), "localService.metric.taskCode");
        requireText(m.taskName(), "localService.metric.taskName");
        requireText(m.windowId(), "localService.metric.windowId");
        requireText(m.windowRole(), "localService.metric.windowRole");
        requireText(m.nativeWindowHandle(), "localService.metric.nativeWindowHandle");
        switch (operation) {
            case METRIC_RECORD_ROUND_STARTED -> {
                requireText(m.roundId(), "METRIC_RECORD_ROUND_STARTED.roundId");
                require(m.status() == null && m.resultCode() == null && m.elapsedMs() == null
                                && m.caseDir() == null && m.reason() == null && m.phase() == null
                                && m.round() == null,
                        "METRIC_RECORD_ROUND_STARTED carries only round-start fields");
            }
            case METRIC_RECORD_ROUND_FINISHED -> {
                requireText(m.roundId(), "METRIC_RECORD_ROUND_FINISHED.roundId");
                requireText(m.status(), "METRIC_RECORD_ROUND_FINISHED.status");
                require(isLegalMetricStatus(m.status()),
                        "METRIC_RECORD_ROUND_FINISHED.status must be a legal AutomationMetricStatus value");
                require(m.elapsedMs() != null && m.elapsedMs() >= 0L,
                        "METRIC_RECORD_ROUND_FINISHED.elapsedMs must be a non-negative value");
                require(m.caseDir() == null && m.reason() == null && m.phase() == null
                                && m.round() == null,
                        "METRIC_RECORD_ROUND_FINISHED carries only round-finish fields");
            }
            case METRIC_RECORD_XIULUO_FAILURE_CASE -> {
                requireText(m.caseDir(), "METRIC_RECORD_XIULUO_FAILURE_CASE.caseDir");
                requireText(m.reason(), "METRIC_RECORD_XIULUO_FAILURE_CASE.reason");
                requireText(m.phase(), "METRIC_RECORD_XIULUO_FAILURE_CASE.phase");
                require(m.round() != null, "METRIC_RECORD_XIULUO_FAILURE_CASE.round must not be null");
                require(m.roundId() == null && m.roundNumber() == null && m.roundType() == null
                                && m.status() == null && m.resultCode() == null && m.elapsedMs() == null,
                        "METRIC_RECORD_XIULUO_FAILURE_CASE carries only failure-case fields");
            }
            default -> throw new IllegalStateException("not a metric operation: " + operation);
        }
    }

    /** The wire status is a closed set: only legal AutomationMetricStatus names pass the boundary. */
    private static boolean isLegalMetricStatus(String status) {
        try {
            com.bot.dhxy.model.metrics.AutomationMetricStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void requireWholeTaskRuntime(TurnLocalOperation operation, TurnWholeTaskRuntimeArguments a) {
        requireText(a.source(), "localService.wholeTaskRuntime.source");
        switch (operation) {
            case WHOLE_TASK_PATHING_REGISTER -> {
                require(a.pathingIntent() != null,
                        "WHOLE_TASK_PATHING_REGISTER requires pathingIntent");
                requireText(a.pathingIntent().intentId(),
                        "WHOLE_TASK_PATHING_REGISTER.pathingIntent.intentId");
                requireText(a.pathingIntent().source(),
                        "WHOLE_TASK_PATHING_REGISTER.pathingIntent.source");
                requireText(a.pathingIntent().type(),
                        "WHOLE_TASK_PATHING_REGISTER.pathingIntent.type");
            }
            case WHOLE_TASK_PATHING_CLEAR_INTENT ->
                    requireText(a.intentId(), "WHOLE_TASK_PATHING_CLEAR_INTENT.intentId");
            case WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX ->
                    requireText(a.sourcePrefix(), "WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX.sourcePrefix");
            case WHOLE_TASK_COMBAT_ENTRY_CLEANUP -> {
                requireText(a.taskCode(), "WHOLE_TASK_COMBAT_ENTRY_CLEANUP.taskCode");
                requireText(a.sourcePrefix(), "WHOLE_TASK_COMBAT_ENTRY_CLEANUP.sourcePrefix");
            }
            case WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP -> {
                requireText(a.intentId(), "WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP.intentId");
                requireText(a.targetMapName(), "WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP.targetMapName");
            }
            case WHOLE_TASK_MOVEMENT_INTENT_RECORD ->
                    require(a.protectionMs() == null || a.protectionMs() >= 0L,
                            "WHOLE_TASK_MOVEMENT_INTENT_RECORD.protectionMs must not be negative");
            case WHOLE_TASK_TARGET_MAP_GATE_START -> {
                requireText(a.taskCode(), "WHOLE_TASK_TARGET_MAP_GATE_START.taskCode");
                requireText(a.targetMapName(), "WHOLE_TASK_TARGET_MAP_GATE_START.targetMapName");
            }
            case WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST -> {
                requireText(a.taskCode(), "WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST.taskCode");
                require(a.interestOperations() != null && !a.interestOperations().isEmpty(),
                        "WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST requires nonempty interestOperations");
                require(a.absentAllowedAtMs() == null || a.absentAllowedAtMs() >= 0L,
                        "WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST.absentAllowedAtMs must not be negative");
            }
            case WHOLE_TASK_PRE_BATTLE_TIMER_START ->
                    requireText(a.taskCode(), "WHOLE_TASK_PRE_BATTLE_TIMER_START.taskCode");
            case WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE ->
                    require(a.blockedMs() != null && a.blockedMs() >= 0L,
                            "WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE.blockedMs must be nonnegative");
            case WHOLE_TASK_DIALOG_INTEREST_UPDATE -> {
                requireText(a.taskCode(), "WHOLE_TASK_DIALOG_INTEREST_UPDATE.taskCode");
                require(a.interestOperations() != null && !a.interestOperations().isEmpty(),
                        "WHOLE_TASK_DIALOG_INTEREST_UPDATE requires nonempty interestOperations");
                require(a.absentAllowedAtMs() == null || a.absentAllowedAtMs() >= 0L,
                        "WHOLE_TASK_DIALOG_INTEREST_UPDATE.absentAllowedAtMs must not be negative");
                require(a.probeStartAtMs() == null || a.probeStartAtMs() > 0L,
                        "WHOLE_TASK_DIALOG_INTEREST_UPDATE.probeStartAtMs must be positive when present");
                // TURN-40G repair review: the four schedule identity fields are all-or-none — a partial identity
                // is rejected on the wire so the client can never install a fabricated attempt.
                boolean anySchedule = a.scheduleAttemptId() != null || a.scheduleRound() != null
                        || a.scheduleTaskRunId() != null || a.scheduleOpenedAtMs() != null;
                if (anySchedule) {
                    requireText(a.scheduleAttemptId(), "WHOLE_TASK_DIALOG_INTEREST_UPDATE.scheduleAttemptId");
                    require(a.scheduleRound() != null && a.scheduleRound() > 0,
                            "WHOLE_TASK_DIALOG_INTEREST_UPDATE.scheduleRound must be present and positive (rounds are one-based)");
                    requireText(a.scheduleTaskRunId(), "WHOLE_TASK_DIALOG_INTEREST_UPDATE.scheduleTaskRunId");
                    require(a.scheduleOpenedAtMs() != null && a.scheduleOpenedAtMs() > 0L,
                            "WHOLE_TASK_DIALOG_INTEREST_UPDATE.scheduleOpenedAtMs must be present and positive");
                }
            }
            case WHOLE_TASK_PROGRESS_UPDATE -> require(
                    a.completedRuns() != null && a.completedRuns() >= 0 && a.totalRuns() != null,
                    "WHOLE_TASK_PROGRESS_UPDATE requires nonnegative completedRuns and totalRuns");
            case WHOLE_TASK_DIALOG_RUNTIME_READ -> require(
                    a.dialogSnapshotMaxAgeMs() == null || a.dialogSnapshotMaxAgeMs() >= 0L,
                    "WHOLE_TASK_DIALOG_RUNTIME_READ requires a nonnegative dialogSnapshotMaxAgeMs when present");
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE -> require(
                    a.transferChoice() != null,
                    "WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE requires a transferChoice payload");
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME -> {
                requireText(a.intentId(), operation + ".intentId");
                requireText(a.sourcePrefix(), operation + ".sourcePrefix");
            }
            case WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE -> {
                require(a.routeOutcome() != null,
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE requires a routeOutcome payload");
                require("YELLOW_DESTINATION_MINI_MAP".equals(a.routeOutcome().routeMode()),
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE.routeOutcome.routeMode must be "
                                + "YELLOW_DESTINATION_MINI_MAP");
                requireText(a.routeOutcomeReplacementReason(),
                        "WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE.routeOutcomeReplacementReason");
            }
            case WHOLE_TASK_RETURN_HOME_REPLAY_ARM -> {
                requireText(a.taskCode(), "WHOLE_TASK_RETURN_HOME_REPLAY_ARM.taskCode");
                requireText(a.replayObservationRunId(),
                        "WHOLE_TASK_RETURN_HOME_REPLAY_ARM.replayObservationRunId");
                requireText(a.replayBusinessTaskRunId(),
                        "WHOLE_TASK_RETURN_HOME_REPLAY_ARM.replayBusinessTaskRunId");
                require("XIULUO_V2".equalsIgnoreCase(a.taskCode())
                                || "XINSHOU_TRAINING".equalsIgnoreCase(a.taskCode())
                                || "CATCH_GHOST".equalsIgnoreCase(a.taskCode())
                                || "WUBEI".equalsIgnoreCase(a.taskCode()),
                        "WHOLE_TASK_RETURN_HOME_REPLAY_ARM supports only XIULUO_V2/XINSHOU_TRAINING/CATCH_GHOST/WUBEI");
            }
            case WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM -> {
                requireText(a.taskCode(), "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM.taskCode");
                requireText(a.expectedCombatClaimId(), "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM.claimId");
                requireText(a.expectedCombatObservationRunId(),
                        "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM.observationRunId");
                requireText(a.expectedCombatBusinessTaskRunId(),
                        "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM.businessTaskRunId");
                requireText(a.expectedCombatAttemptId(), "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM.attemptId");
                require("XIULUO_V2".equalsIgnoreCase(a.taskCode())
                                || "XINSHOU_TRAINING".equalsIgnoreCase(a.taskCode())
                                || "CATCH_GHOST".equalsIgnoreCase(a.taskCode())
                                || "TIANTING".equalsIgnoreCase(a.taskCode())
                                || "WUBEI".equalsIgnoreCase(a.taskCode()),
                        "WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM supports only XIULUO_V2/XINSHOU_TRAINING/CATCH_GHOST/TIANTING/WUBEI");
            }
            case WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME -> {
                requireText(a.intentId(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.intentId");
                requireText(a.taskCode(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.taskCode");
                requireText(a.targetKeyword(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.targetKeyword");
                TurnNpcArrivalFrameFifoSpec spec = a.npcArrivalFifo();
                require(spec != null, "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME requires npcArrivalFifo");
                requireText(spec.tenantId(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.tenantId");
                requireText(spec.deviceId(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.deviceId");
                requireText(spec.windowId(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.windowId");
                requireText(spec.hwnd(), "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.hwnd");
                requireText(spec.observationRunId(),
                        "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.observationRunId");
                requireText(spec.businessTaskRunId(),
                        "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME.businessTaskRunId");
                require(spec.allowedLeft() >= 0 && spec.allowedTop() >= 0
                                && spec.allowedWidth() > 0 && spec.allowedHeight() > 0
                                && (long) spec.allowedLeft() + spec.allowedWidth() <= 1024
                                && (long) spec.allowedTop() + spec.allowedHeight() <= 768,
                        "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME allowed region must be inside 1024x768");
                require(spec.expectedDialogTemplatePaths() != null,
                        "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME expected templates must be present");
                spec.expectedDialogTemplatePaths().forEach(path ->
                        requireText(path, "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME expected template"));
            }
            case WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE ->
                    require(a.startupFlyingState() != null
                                    && ("FLYING".equals(a.startupFlyingState())
                                    || "NOT_FLYING".equals(a.startupFlyingState())
                                    || "UNKNOWN".equals(a.startupFlyingState())),
                            "WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE requires a valid startupFlyingState");
            case WHOLE_TASK_RECOVERY_RESET -> {
                boolean anyExactIdentity = a.recoveryTaskRunId() != null
                        || a.recoveryRound() != null
                        || a.recoveryAttemptId() != null;
                if (anyExactIdentity) {
                    requireText(a.recoveryTaskRunId(), "WHOLE_TASK_RECOVERY_RESET.taskRunId");
                    require(a.recoveryRound() != null && a.recoveryRound() > 0,
                            "WHOLE_TASK_RECOVERY_RESET.round must be positive");
                    requireText(a.recoveryAttemptId(), "WHOLE_TASK_RECOVERY_RESET.attemptId");
                }
            }
            case WHOLE_TASK_PATHING_CLEAR,
                 WHOLE_TASK_PATHING_READ,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ,
                 WHOLE_TASK_TARGET_MAP_GATE_OPEN,
                 WHOLE_TASK_PRE_BATTLE_TIMER_READ,
                 WHOLE_TASK_PRE_BATTLE_FACT_READ,
                 WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK,
                 WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR,
                 WHOLE_TASK_DIALOG_INTEREST_CLEAR,
                 WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME,
                 XIULUO_ACCEPT_DIALOG_TEMPLATE,
                 JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE,
                 CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE,
                 CATCH_GHOST_CANCEL_DIALOG_TEMPLATE -> {
                // source-only operations: no additional payload fields required.
            }
            default -> throw new IllegalArgumentException(
                    "non-whole-task operation routed to requireWholeTaskRuntime: " + operation);
        }
        // Internal exactly-one payload closure: reject every present field outside this operation's
        // own payload, so a source-only or clear operation can never smuggle another op's fields.
        EnumSet<WtField> extras = presentFields(a);
        extras.removeAll(allowedWholeTaskFields(operation));
        require(extras.isEmpty(),
                operation + " carries wholeTaskRuntime fields outside its payload: " + extras);
    }

    private enum WtField {
        PATHING_INTENT, INTENT_ID, SOURCE_PREFIX, PROTECTION_MS, CURRENT_MAP, CURRENT_X, CURRENT_Y,
        TARGET_MAP, TARGET_X, TARGET_Y, TOLERANCE, CONFIRM_TIMEOUT, TASK_CODE, TARGET_KEYWORD,
        BLOCKED_MS, INTEREST_OPS, ABSENT_ALLOWED, PROBE_ONLY, COMPLETED_RUNS, TOTAL_RUNS,
        DIALOG_MAX_AGE, TRANSFER_CHOICE, ROUTE_OUTCOME, ROUTE_OUTCOME_REASON, STARTUP_FLYING_STATE,
        PROBE_START_AT, SCHEDULE_ATTEMPT, SCHEDULE_ROUND, SCHEDULE_TASK_RUN, SCHEDULE_OPENED_AT,
        SCHEDULE_OBSERVATION_RUN,
        REPLAY_OBSERVATION_RUN, REPLAY_BUSINESS_TASK_RUN, EXPECTED_CLAIM_ID,
        EXPECTED_OBSERVATION_RUN, EXPECTED_BUSINESS_TASK_RUN, EXPECTED_ATTEMPT_ID,
        RECOVERY_TASK_RUN, RECOVERY_ROUND, RECOVERY_ATTEMPT,
        NPC_ARRIVAL_FIFO
    }

    private static EnumSet<WtField> presentFields(TurnWholeTaskRuntimeArguments a) {
        EnumSet<WtField> present = EnumSet.noneOf(WtField.class);
        if (a.pathingIntent() != null) present.add(WtField.PATHING_INTENT);
        if (a.intentId() != null) present.add(WtField.INTENT_ID);
        if (a.sourcePrefix() != null) present.add(WtField.SOURCE_PREFIX);
        if (a.protectionMs() != null) present.add(WtField.PROTECTION_MS);
        if (a.currentMapName() != null) present.add(WtField.CURRENT_MAP);
        if (a.currentX() != null) present.add(WtField.CURRENT_X);
        if (a.currentY() != null) present.add(WtField.CURRENT_Y);
        if (a.targetMapName() != null) present.add(WtField.TARGET_MAP);
        if (a.targetX() != null) present.add(WtField.TARGET_X);
        if (a.targetY() != null) present.add(WtField.TARGET_Y);
        if (a.tolerance() != null) present.add(WtField.TOLERANCE);
        if (a.confirmTimeoutMs() != null) present.add(WtField.CONFIRM_TIMEOUT);
        if (a.taskCode() != null) present.add(WtField.TASK_CODE);
        if (a.targetKeyword() != null) present.add(WtField.TARGET_KEYWORD);
        if (a.blockedMs() != null) present.add(WtField.BLOCKED_MS);
        if (a.interestOperations() != null) present.add(WtField.INTEREST_OPS);
        if (a.absentAllowedAtMs() != null) present.add(WtField.ABSENT_ALLOWED);
        if (a.probeOnly() != null) present.add(WtField.PROBE_ONLY);
        if (a.completedRuns() != null) present.add(WtField.COMPLETED_RUNS);
        if (a.totalRuns() != null) present.add(WtField.TOTAL_RUNS);
        if (a.dialogSnapshotMaxAgeMs() != null) present.add(WtField.DIALOG_MAX_AGE);
        if (a.transferChoice() != null) present.add(WtField.TRANSFER_CHOICE);
        if (a.routeOutcome() != null) present.add(WtField.ROUTE_OUTCOME);
        if (a.routeOutcomeReplacementReason() != null) present.add(WtField.ROUTE_OUTCOME_REASON);
        if (a.startupFlyingState() != null) present.add(WtField.STARTUP_FLYING_STATE);
        if (a.probeStartAtMs() != null) present.add(WtField.PROBE_START_AT);
        if (a.scheduleAttemptId() != null) present.add(WtField.SCHEDULE_ATTEMPT);
        if (a.scheduleRound() != null) present.add(WtField.SCHEDULE_ROUND);
        if (a.scheduleTaskRunId() != null) present.add(WtField.SCHEDULE_TASK_RUN);
        if (a.scheduleOpenedAtMs() != null) present.add(WtField.SCHEDULE_OPENED_AT);
        if (a.scheduleObservationRunId() != null) present.add(WtField.SCHEDULE_OBSERVATION_RUN);
        if (a.replayObservationRunId() != null) present.add(WtField.REPLAY_OBSERVATION_RUN);
        if (a.replayBusinessTaskRunId() != null) present.add(WtField.REPLAY_BUSINESS_TASK_RUN);
        if (a.expectedCombatClaimId() != null) present.add(WtField.EXPECTED_CLAIM_ID);
        if (a.expectedCombatObservationRunId() != null) present.add(WtField.EXPECTED_OBSERVATION_RUN);
        if (a.expectedCombatBusinessTaskRunId() != null) present.add(WtField.EXPECTED_BUSINESS_TASK_RUN);
        if (a.expectedCombatAttemptId() != null) present.add(WtField.EXPECTED_ATTEMPT_ID);
        if (a.recoveryTaskRunId() != null) present.add(WtField.RECOVERY_TASK_RUN);
        if (a.recoveryRound() != null) present.add(WtField.RECOVERY_ROUND);
        if (a.recoveryAttemptId() != null) present.add(WtField.RECOVERY_ATTEMPT);
        if (a.npcArrivalFifo() != null) present.add(WtField.NPC_ARRIVAL_FIFO);
        return present;
    }

    private static EnumSet<WtField> allowedWholeTaskFields(TurnLocalOperation operation) {
        return switch (operation) {
            case WHOLE_TASK_PATHING_REGISTER -> EnumSet.of(WtField.PATHING_INTENT);
            case WHOLE_TASK_PATHING_CLEAR_INTENT -> EnumSet.of(WtField.INTENT_ID);
            case WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX -> EnumSet.of(WtField.SOURCE_PREFIX);
            case WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP -> EnumSet.of(WtField.INTENT_ID, WtField.TARGET_MAP);
            case WHOLE_TASK_MOVEMENT_INTENT_RECORD -> EnumSet.of(WtField.PROTECTION_MS);
            case WHOLE_TASK_TARGET_MAP_GATE_START -> EnumSet.of(WtField.TASK_CODE, WtField.TARGET_MAP);
            case WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST -> EnumSet.of(
                    WtField.TASK_CODE, WtField.INTEREST_OPS, WtField.ABSENT_ALLOWED, WtField.PROBE_ONLY);
            case WHOLE_TASK_COMBAT_ENTRY_CLEANUP -> EnumSet.of(WtField.TASK_CODE, WtField.SOURCE_PREFIX);
            case WHOLE_TASK_PRE_BATTLE_TIMER_START -> EnumSet.of(WtField.TASK_CODE, WtField.TARGET_KEYWORD);
            case WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE -> EnumSet.of(WtField.BLOCKED_MS);
            case WHOLE_TASK_DIALOG_INTEREST_UPDATE -> EnumSet.of(WtField.TASK_CODE, WtField.INTEREST_OPS,
                    WtField.ABSENT_ALLOWED, WtField.PROBE_ONLY, WtField.PROBE_START_AT,
                    WtField.SCHEDULE_ATTEMPT, WtField.SCHEDULE_ROUND, WtField.SCHEDULE_TASK_RUN,
                    WtField.SCHEDULE_OPENED_AT, WtField.SCHEDULE_OBSERVATION_RUN);
            case WHOLE_TASK_PROGRESS_UPDATE -> EnumSet.of(WtField.COMPLETED_RUNS, WtField.TOTAL_RUNS);
            case WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE -> EnumSet.of(WtField.STARTUP_FLYING_STATE);
            case WHOLE_TASK_DIALOG_RUNTIME_READ -> EnumSet.of(WtField.DIALOG_MAX_AGE);
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE -> EnumSet.of(WtField.TRANSFER_CHOICE);
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                 WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME -> EnumSet.of(WtField.INTENT_ID, WtField.SOURCE_PREFIX);
            case WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE ->
                    EnumSet.of(WtField.ROUTE_OUTCOME, WtField.ROUTE_OUTCOME_REASON);
            case WHOLE_TASK_RETURN_HOME_REPLAY_ARM ->
                    EnumSet.of(WtField.TASK_CODE, WtField.REPLAY_OBSERVATION_RUN,
                            WtField.REPLAY_BUSINESS_TASK_RUN);
            case WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM ->
                    EnumSet.of(WtField.TASK_CODE, WtField.EXPECTED_CLAIM_ID,
                            WtField.EXPECTED_OBSERVATION_RUN, WtField.EXPECTED_BUSINESS_TASK_RUN,
                             WtField.EXPECTED_ATTEMPT_ID);
            case WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME ->
                    EnumSet.of(WtField.INTENT_ID, WtField.TASK_CODE, WtField.TARGET_KEYWORD,
                            WtField.NPC_ARRIVAL_FIFO);
            case WHOLE_TASK_RECOVERY_RESET -> EnumSet.of(
                    WtField.RECOVERY_TASK_RUN, WtField.RECOVERY_ROUND, WtField.RECOVERY_ATTEMPT);
            case WHOLE_TASK_PATHING_READ, WHOLE_TASK_PATHING_CLEAR, WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ,
                 WHOLE_TASK_PRE_BATTLE_TIMER_READ,
                 WHOLE_TASK_PRE_BATTLE_FACT_READ,
                 WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK, WHOLE_TASK_TARGET_MAP_GATE_OPEN,
                 WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR, WHOLE_TASK_DIALOG_INTEREST_CLEAR,
                 WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME, XIULUO_ACCEPT_DIALOG_TEMPLATE,
                 JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE,
                 CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE,
                 CATCH_GHOST_CANCEL_DIALOG_TEMPLATE ->
                    EnumSet.noneOf(WtField.class);
            default -> throw new IllegalArgumentException(
                    "non-whole-task operation routed to allowedWholeTaskFields: " + operation);
        };
    }

    private static void requireBag(TurnBagOperationArguments bag) {
        require(bag.intent() != null, "localService.bag.intent must not be null");
        requireText(bag.source(), "localService.bag.source");
        boolean anyReplayIdentity = bag.retainedReplayTaskCode() != null
                || bag.retainedReplayObservationRunId() != null
                || bag.retainedReplayBusinessTaskRunId() != null;
        if (anyReplayIdentity) {
            requireText(bag.retainedReplayTaskCode(), "localService.bag.retainedReplayTaskCode");
            requireText(bag.retainedReplayObservationRunId(),
                    "localService.bag.retainedReplayObservationRunId");
            requireText(bag.retainedReplayBusinessTaskRunId(),
                    "localService.bag.retainedReplayBusinessTaskRunId");
                require("XIULUO_V2".equalsIgnoreCase(bag.retainedReplayTaskCode())
                                || "XINSHOU_TRAINING".equalsIgnoreCase(bag.retainedReplayTaskCode())
                                || "CATCH_GHOST".equalsIgnoreCase(bag.retainedReplayTaskCode())
                            || "WUBEI".equalsIgnoreCase(bag.retainedReplayTaskCode()),
                    "retained replay supports only XIULUO_V2/XINSHOU_TRAINING/CATCH_GHOST/WUBEI");
            require(bag.intent() == TurnBagOperationArguments.ReturnItemIntent.USE_CACHED_RETURN_ITEM
                            || bag.intent() == TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                    "retained replay requires a return-item use intent");
        }
        switch (bag.intent()) {
            case PRESCAN_TASK_PAGE -> {
                requireText(bag.targetItemTemplate(), "PRESCAN_TASK_PAGE.targetItemTemplate");
                require(bag.maxBagIndex() != null && bag.maxBagIndex() == -1 && bag.cachedPoint() == null,
                        "PRESCAN_TASK_PAGE requires maxBagIndex=-1 and no cachedPoint");
            }
            case PRESCAN_FROM_BACK -> {
                requireText(bag.targetItemTemplate(), "PRESCAN_FROM_BACK.targetItemTemplate");
                require(bag.maxBagIndex() != null && bag.cachedPoint() == null,
                        "PRESCAN_FROM_BACK requires maxBagIndex and no cachedPoint");
            }
            case USE_CACHED_RETURN_ITEM -> {
                require(bag.targetItemTemplate() == null && bag.maxBagIndex() != null && bag.maxBagIndex() == -1,
                        "USE_CACHED_RETURN_ITEM requires no targetItemTemplate and maxBagIndex=-1");
                if (bag.cachedPoint() != null) {
                    requireCachePoint(bag.cachedPoint());
                }
            }
            case FIND_AND_USE_TASK_PAGE -> {
                requireText(bag.targetItemTemplate(), "FIND_AND_USE_TASK_PAGE.targetItemTemplate");
                require(bag.maxBagIndex() != null && bag.maxBagIndex() == -1 && bag.cachedPoint() == null,
                        "FIND_AND_USE_TASK_PAGE requires maxBagIndex=-1 and no cachedPoint");
            }
        }
    }

    private static void requireCachePoint(TurnReturnItemCachePoint point) {
        requireText(point.templatePath(), "cachedPoint.templatePath");
        require(point.learnedAtMs() >= 0L, "cachedPoint.learnedAtMs must not be negative");
        requireText(point.source(), "cachedPoint.source");
    }

    private static void requireOutcomeFailureShape(TurnOutcome outcome) {
        switch (outcome.status()) {
            case COMPLETED -> {
                require(outcome.failedStepIndex() == null, "COMPLETED must not have failedStepIndex");
                for (TurnStepResult result : outcome.stepResults()) {
                    require(result.status() == TurnStepResult.Status.COMPLETED,
                            "COMPLETED outcome requires every step result to be COMPLETED");
                }
            }
            case FAILED -> {
                require(outcome.failedStepIndex() != null && outcome.failedStepIndex() >= 0,
                        "FAILED outcome requires a nonnegative failedStepIndex");
                require(outcome.failedStepIndex() < outcome.stepResults().size(),
                        "FAILED outcome failedStepIndex must name a step result");
                for (int index = 0; index < outcome.stepResults().size(); index++) {
                    TurnStepResult.Status status = outcome.stepResults().get(index).status();
                    if (index < outcome.failedStepIndex()) {
                        require(status == TurnStepResult.Status.COMPLETED,
                                "FAILED outcome requires preceding step results to be COMPLETED");
                    } else if (index == outcome.failedStepIndex()) {
                        require(status == TurnStepResult.Status.FAILED,
                                "FAILED outcome requires failedStepIndex result to be FAILED");
                    } else {
                        require(status == TurnStepResult.Status.NOT_RUN,
                                "FAILED outcome requires later step results to be NOT_RUN");
                    }
                }
            }
            case STOPPED, DUPLICATE_OR_UNCERTAIN ->
                    require(outcome.failedStepIndex() == null, outcome.status() + " must not have failedStepIndex");
        }
    }

    private static void requireMatchResult(TurnMatchResult match) {
        require(Double.isFinite(match.score()) && match.score() >= 0.0D && match.score() <= 1.0D,
                "match result score must be in [0.0, 1.0]");
        if (match.found()) {
            requirePoint(match.centerX(), match.centerY(), "match result center");
            requireRegion(match.rectangle(), "match result rectangle", false);
        } else {
            require(match.centerX() == null && match.centerY() == null && match.rectangle() == null,
                    "not-found match result must not contain coordinates");
        }
    }

    private static void requireFrame(TurnFrameMetadata frame) {
        require(frame.purpose() != null, "frame.purpose must not be null");
        require("image/png".equalsIgnoreCase(frame.contentType()), "frame.contentType must be image/png");
        requireSha256(frame.sha256(), "frame.sha256");
        require(frame.width() > 0 && frame.height() > 0, "frame dimensions must be positive");
        requireRegion(frame.region(), "frame.region", false);
        require(frame.region().width() == frame.width() && frame.region().height() == frame.height(),
                "frame dimensions must match frame.region");
        if (frame.sourceStepIndex() != null) {
            require(frame.sourceStepIndex() >= 0, "frame.sourceStepIndex must not be negative");
        }
    }

    private static void requireWindow(TurnWindowMetadata window, String field) {
        require(window != null, field + " must not be null");
        requireText(window.deviceId(), field + ".deviceId");
        requireText(window.windowId(), field + ".windowId");
        requireText(window.windowTitle(), field + ".windowTitle");
        requireText(window.nativeHandle(), field + ".nativeHandle");
        require(window.processId() > 0L, field + ".processId must be positive");
        require(window.windowRect() != null, field + ".windowRect must not be null");
        require(window.windowRect().width() > 0 && window.windowRect().height() > 0,
                field + ".windowRect dimensions must be positive");
        if (window.pathingSnapshot() != null) {
            requireText(window.pathingSnapshot().state(), field + ".pathingSnapshot.state");
            TurnPathingIntent snapshotIntent = window.pathingSnapshot().intent();
            if (snapshotIntent != null) {
                requireText(snapshotIntent.source(), field + ".pathingSnapshot.intent.source");
                requireText(snapshotIntent.intentId(), field + ".pathingSnapshot.intent.intentId");
                require(snapshotIntent.tolerance() >= 0,
                        field + ".pathingSnapshot.intent.tolerance must not be negative");
            }
        }
    }

    private static void requireTaskStartRequest(TurnTaskStartRequest request) {
        requireText(request.startRequestId(), "taskStartRequest.startRequestId");
        require(request.taskCodes() != null && !request.taskCodes().isEmpty(),
                "taskStartRequest.taskCodes must not be empty");
        for (int index = 0; index < request.taskCodes().size(); index++) {
            TurnTaskCode taskCode = request.taskCodes().get(index);
            require(taskCode != null,
                    "taskStartRequest.taskCodes[" + index + "] must not be null");
            require(request.taskMaxRuns() != null,
                    "taskStartRequest.taskMaxRuns must not be null");
            require(request.taskMaxRuns().size() == request.taskCodes().size(),
                    "taskStartRequest.taskMaxRuns size must equal taskCodes size");
            Integer maxRuns = request.taskMaxRuns().get(index);
            require(maxRuns != null,
                    "taskStartRequest.taskMaxRuns[" + index + "] must not be null");
            switch (taskCode) {
                case WUBEI, XIULUO_V2, XINSHOU_TRAINING, CATCH_GHOST, WILD_BATTLE, AUTO_BATTLE, TIANTING -> require(maxRuns >= 0,
                        "taskStartRequest.taskMaxRuns[" + index + "] must be >= 0 for " + taskCode);
                case WUHUAN_V2 -> require(maxRuns == 1 || maxRuns == 2,
                        "taskStartRequest.taskMaxRuns[" + index + "] must be 1 or 2 for " + taskCode);
                case XINSHOU, SLEEP_COMPUTER, YIPIN_GUARD_TEST -> require(maxRuns == 1,
                        "taskStartRequest.taskMaxRuns[" + index + "] must be 1 for " + taskCode);
            }
        }
        require(request.failurePolicy() != null, "taskStartRequest.failurePolicy must not be null");
    }

    /**
     * A window carrying a {@code taskStartRequest} must project the exact baseline role/team/startup authority
     * facts. The facts must be present and self-consistent per the local runner's baseline invariants — a
     * present team session or leader window id or support member each requires a present leader, and a present
     * leader requires a team session — and {@code startupMode} must name an existing {@link TaskStartupMode}.
     */
    private static void requireTaskStartWindowAuthority(TurnWindowMetadata window) {
        requireText(window.windowRole(), "taskStartRequest.window.windowRole");
        requireText(window.startupMode(), "taskStartRequest.window.startupMode");
        requireStartupMode(window.startupMode());
        require(window.localLeaderPresent() != null,
                "taskStartRequest.window.localLeaderPresent must not be missing");
        require(window.localSupportMember() != null,
                "taskStartRequest.window.localSupportMember must not be missing");
        boolean leaderPresent = window.localLeaderPresent();
        boolean supportMember = window.localSupportMember();
        boolean hasSession = isPresent(window.localTeamSessionKey());
        boolean hasLeaderWindow = isPresent(window.localLeaderWindowId());
        require(hasSession == leaderPresent,
                "taskStartRequest.window local team facts are contradictory: "
                        + "localTeamSessionKey presence must equal localLeaderPresent");
        require(!hasLeaderWindow || leaderPresent,
                "taskStartRequest.window.localLeaderWindowId requires localLeaderPresent");
        require(!supportMember || leaderPresent,
                "taskStartRequest.window.localSupportMember requires localLeaderPresent");
        requireLocalTeamRolePreflightFacts(window);
    }

    /**
     * Validate CR212 repaired local preflight as one all-or-none fact bundle.
     *
     * <p>A completed preflight may report solo/unknown without a tooltip group. A grouped preflight carries one
     * batch session and group hash for every group member, but only the representative carries the mask PNG.
     * Cloud must never treat a partial bundle as permission to hover/capture the game window a second time.</p>
     */
    private static void requireLocalTeamRolePreflightFacts(TurnWindowMetadata window) {
        boolean complete = Boolean.TRUE.equals(window.localTeamRolePreflightComplete());
        boolean hasAny = window.localTeamRolePreflightComplete() != null
                || isPresent(window.localTeamRolePreflightSessionKey())
                || isPresent(window.localTeamRoleTooltipGroupHash())
                || isPresent(window.localTeamRoleTooltipMaskBase64())
                || window.localTeamRoleTooltipRepresentative() != null;
        if (!complete) {
            require(!hasAny, "taskStartRequest.window local team-role preflight facts must be all-or-none");
            return;
        }
        requireText(window.localTeamRolePreflightSessionKey(),
                "taskStartRequest.window.localTeamRolePreflightSessionKey");
        require(window.localTeamRoleTooltipRepresentative() != null,
                "taskStartRequest.window.localTeamRoleTooltipRepresentative must not be missing");
        requireKnownTeamRole(window.windowRole());
        boolean hasGroup = isPresent(window.localTeamRoleTooltipGroupHash());
        boolean representative = window.localTeamRoleTooltipRepresentative();
        if (!hasGroup) {
            require(!representative && !isPresent(window.localTeamRoleTooltipMaskBase64()),
                    "solo/unknown local preflight must not carry representative tooltip payload");
            return;
        }
        if (representative) {
            requireText(window.localTeamRoleTooltipMaskBase64(),
                    "taskStartRequest.window.localTeamRoleTooltipMaskBase64");
        } else {
            require(!isPresent(window.localTeamRoleTooltipMaskBase64()),
                    "non-representative local preflight must not carry tooltip mask");
        }
    }

    private static void requireKnownTeamRole(String windowRole) {
        require("LEADER".equals(windowRole) || "MEMBER".equals(windowRole)
                        || "SOLO".equals(windowRole) || "UNKNOWN".equals(windowRole),
                "taskStartRequest.window.windowRole must be LEADER, MEMBER, SOLO, or UNKNOWN for local preflight");
    }

    private static void requireStartupMode(String startupMode) {
        try {
            TaskStartupMode.valueOf(startupMode);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "taskStartRequest.window.startupMode is not a valid TaskStartupMode: " + startupMode);
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static void requireTaskStartAck(TurnTaskStartAck ack) {
        requireText(ack.startRequestId(), "taskStartAck.startRequestId");
        if (ack.effectiveTaskCodes() != null) {
            for (TurnTaskCode code : ack.effectiveTaskCodes()) {
                require(code != null, "taskStartAck.effectiveTaskCodes must not contain null");
            }
        }
    }

    private static void requireSameWindow(TurnWindowMetadata expected, TurnWindowMetadata actual, String field) {
        require(expected.deviceId().equals(actual.deviceId()) && expected.windowId().equals(actual.windowId()),
                field + " must match request deviceId and windowId");
    }

    private static void requireRegion(TurnRegion region, String field, boolean nullable) {
        if (region == null) {
            require(nullable, field + " must not be null");
            return;
        }
        require(region.width() > 0 && region.height() > 0, field + " dimensions must be positive");
    }

    private static boolean isInsidePaddedRegion(int x, int y, TurnRegion region, int paddingPx) {
        long left = (long) region.x() - paddingPx;
        long top = (long) region.y() - paddingPx;
        long right = (long) region.x() + region.width() + paddingPx;
        long bottom = (long) region.y() + region.height() + paddingPx;
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private static boolean isInsideRegion(int x, int y, TurnRegion region) {
        long right = (long) region.x() + region.width();
        long bottom = (long) region.y() + region.height();
        return x >= region.x() && x < right && y >= region.y() && y < bottom;
    }

    private static void requireClickTiming(Integer value, String field) {
        require(value == null || value >= 0 && value <= 5_000, field + " must be in [0, 5000]");
    }

    private static void requirePoint(Integer x, Integer y, String field) {
        require(x != null && y != null, field + " x and y must not be null");
    }

    private static void requireContractVersion(int contractVersion) {
        require(contractVersion == CONTRACT_VERSION, "contractVersion must be " + CONTRACT_VERSION);
    }

    private static void requireSha256(String value, String field) {
        require(value != null && value.matches("[0-9a-fA-F]{64}"), field + " must be a SHA-256 hex string");
    }

    private static void requireText(String value, String field) {
        require(value != null && !value.isBlank(), field + " must be nonblank");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
