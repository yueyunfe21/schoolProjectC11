package com.bot.dhxy.cloud.turn.protocol;

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
        }
        return request;
    }

    public static TurnResponse requireValid(TurnResponse response) {
        require(response != null, "response must not be null");
        require(response.status() != null, "response.status must not be null");
        switch (response.status()) {
            case ACTION -> require(response.action() != null, "ACTION response requires action");
            case IDLE -> require(response.action() == null, "IDLE response must not contain action");
        }
        if (response.action() != null) {
            requireValid(response.action());
        }
        if (response.taskStartAck() != null) {
            requireTaskStartAck(response.taskStartAck());
        }
        return response;
    }

    public static TurnResponse requireValid(TurnResponse response, TurnRequest request) {
        TurnRequest validRequest = requireValid(request);
        TurnResponse validResponse = requireValid(response);
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
        return validResponse;
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
        return switch (call.operation()) {
            case BAG_RETURN_ITEM -> {
                require(call.bag() != null && call.ui() == null && call.giveItem() == null && call.quest() == null,
                        "BAG_RETURN_ITEM requires only bag arguments");
                requireBag(call.bag());
                yield 0;
            }
            case BAG_USE_INCENSE, UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS -> {
                require(call.bag() == null && call.ui() == null && call.giveItem() == null && call.quest() == null,
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
        };
    }

    private static void requireBag(TurnBagOperationArguments bag) {
        require(bag.intent() != null, "localService.bag.intent must not be null");
        requireText(bag.source(), "localService.bag.source");
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
            require(request.taskCodes().get(index) != null,
                    "taskStartRequest.taskCodes[" + index + "] must not be null");
        }
        require(request.failurePolicy() != null, "taskStartRequest.failurePolicy must not be null");
    }

    private static void requireTaskStartAck(TurnTaskStartAck ack) {
        requireText(ack.startRequestId(), "taskStartAck.startRequestId");
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
