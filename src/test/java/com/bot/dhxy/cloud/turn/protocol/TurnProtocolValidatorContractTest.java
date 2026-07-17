package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnProtocolValidatorContractTest {

    @Test
    void strictContractMapperRejectsUnknownFieldUnknownEnumAndNullPrimitive() throws IOException {
        ObjectNode unknownField = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.fixtureBytes("action-input-capture.json"));
        unknownField.put("unexpectedField", true);
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(unknownField), TurnAction.class));

        ObjectNode unknownEnum = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.fixtureBytes("action-input-capture.json"));
        ((ObjectNode) unknownEnum.withArray("steps").get(0)).put("inputAction", "TELEPORT");
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(unknownEnum), TurnAction.class));

        ObjectNode nullPrimitive = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.fixtureBytes("action-input-capture.json"));
        nullPrimitive.putNull("contractVersion");
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(nullPrimitive), TurnAction.class));

        ObjectNode unknownOutcomeStatus = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.fixtureBytes("outcome-completed.json"));
        unknownOutcomeStatus.put("status", "BUSY");
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(unknownOutcomeStatus),
                TurnOutcome.class));

        String trailingDocument = new String(TurnProtocolGoldenSupport.fixtureBytes("action-input-capture.json"),
                StandardCharsets.UTF_8) + "\n{}";
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                trailingDocument, TurnAction.class));
    }

    @Test
    void moveMouseRequiresExactlyXYAndRejectsExtraJsonFields() throws IOException {
        TurnAction valid = TurnProtocolGoldenSupport.action(
                "move-mouse-validator",
                List.of(TurnProtocolGoldenSupport.inputStep(
                        0,
                        TurnInputAction.MOVE_MOUSE,
                        new TurnInputSpec(1420, 736, null, null, null, null, null))));
        assertEquals(valid, TurnProtocolValidator.requireValid(valid));

        List<TurnInputSpec> invalidShapes = List.of(
                new TurnInputSpec(null, 736, null, null, null, null, null),
                new TurnInputSpec(1420, null, null, null, null, null, null),
                new TurnInputSpec(1420, 736, 1430, null, null, null, null),
                new TurnInputSpec(1420, 736, null, 746, null, null, null),
                new TurnInputSpec(1420, 736, null, null, 1, null, null),
                new TurnInputSpec(1420, 736, null, null, null, "Q", null),
                new TurnInputSpec(1420, 736, null, null, null, null, "extra"));
        for (TurnInputSpec invalid : invalidShapes) {
            TurnAction invalidAction = TurnProtocolGoldenSupport.action(
                    "move-mouse-invalid",
                    List.of(TurnProtocolGoldenSupport.inputStep(0, TurnInputAction.MOVE_MOUSE, invalid)));
            assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(invalidAction));
        }

        ObjectNode unknownInputField = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(valid);
        ((ObjectNode) unknownInputField.withArray("steps").get(0).get("input")).put("delayMs", 25);
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(unknownInputField),
                TurnAction.class));
    }

    @Test
    void clickTimingIsBoundedAndRejectedByEveryOtherInputAction() {
        for (TurnInputAction click : List.of(TurnInputAction.CLICK_LEFT, TurnInputAction.CLICK_RIGHT)) {
            TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                    "click-timing-min-" + click,
                    List.of(TurnProtocolGoldenSupport.inputStep(
                            0, click, inputFor(click, 0, 0)))));
            TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                    "click-timing-max-" + click,
                    List.of(TurnProtocolGoldenSupport.inputStep(
                            0, click, inputFor(click, 5_000, 5_000)))));
            for (TurnInputSpec invalid : List.of(
                    inputFor(click, -1, null),
                    inputFor(click, 5_001, null),
                    inputFor(click, null, -1),
                    inputFor(click, null, 5_001))) {
                TurnAction invalidAction = TurnProtocolGoldenSupport.action(
                        "click-timing-invalid-" + click,
                        List.of(TurnProtocolGoldenSupport.inputStep(0, click, invalid)));
                assertThrows(IllegalArgumentException.class,
                        () -> TurnProtocolValidator.requireValid(invalidAction));
            }
        }

        for (TurnInputAction disallowed : TurnInputAction.values()) {
            if (disallowed == TurnInputAction.CLICK_LEFT || disallowed == TurnInputAction.CLICK_RIGHT) {
                continue;
            }
            TurnAction invalidAction = TurnProtocolGoldenSupport.action(
                    "timing-rejected-" + disallowed,
                    List.of(TurnProtocolGoldenSupport.inputStep(
                            0, disallowed, inputFor(disallowed, 1, 1))));
            assertThrows(IllegalArgumentException.class,
                    () -> TurnProtocolValidator.requireValid(invalidAction),
                    "timing must be rejected for " + disallowed);
        }
    }

    @Test
    void pixelChangeProbeJsonGeometryAndSingleStepShapeRemainStrictAndClosed() throws IOException {
        TurnAction fixture = TurnProtocolGoldenSupport.readFixture(
                "action-capture-pixel-change.json", TurnAction.class);
        assertEquals(fixture, TurnProtocolValidator.requireValid(fixture));
        ObjectNode validJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(fixture);

        for (String missing : List.of(
                "targetX",
                "targetY",
                "ctrlDownSettleMs",
                "afterMoveSettleMs",
                "ctrlUpSettleMs",
                "differenceRatioThreshold")) {
            ObjectNode missingKey = validJson.deepCopy();
            probeNode(missingKey).remove(missing);
            assertThrows(JsonProcessingException.class,
                    () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                            TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(missingKey),
                            TurnAction.class),
                    "missing pixelChangeProbe key must fail: " + missing);
        }

        ObjectNode extraKey = validJson.deepCopy();
        probeNode(extraKey).put("modifierKey", "CONTROL");
        assertThrows(JsonProcessingException.class,
                () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                        TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(extraKey),
                        TurnAction.class));
        for (String primitive : List.of(
                "targetX",
                "targetY",
                "ctrlDownSettleMs",
                "afterMoveSettleMs",
                "ctrlUpSettleMs",
                "differenceRatioThreshold")) {
            ObjectNode nullPrimitive = validJson.deepCopy();
            probeNode(nullPrimitive).putNull(primitive);
            assertThrows(JsonProcessingException.class,
                    () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                            TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(nullPrimitive),
                            TurnAction.class));
        }

        TurnRegion region = new TurnRegion(200, 300, 100, 80);
        TurnProtocolValidator.requireValid(probeAction(
                region, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 0, 0, 0, 0.0D)));
        TurnProtocolValidator.requireValid(probeAction(
                region, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                new TurnCaptureSpec.PixelChangeProbe(299, 379, 5_000, 5_000, 5_000, 1.0D)));

        for (TurnCaptureSpec.PixelChangeProbe invalid : List.of(
                new TurnCaptureSpec.PixelChangeProbe(199, 300, 80, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(300, 300, 80, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 299, 80, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 380, 80, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, -1, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 5_001, 280, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, -1, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 5_001, 100, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, -1, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, 5_001, 0.05D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, 100, -0.01D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, 100, 1.01D),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, 100, Double.NaN),
                new TurnCaptureSpec.PixelChangeProbe(200, 300, 80, 280, 100, Double.POSITIVE_INFINITY))) {
            assertThrows(IllegalArgumentException.class,
                    () -> TurnProtocolValidator.requireValid(probeAction(
                            region, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE, invalid)));
        }

        TurnCaptureSpec.PixelChangeProbe validProbe =
                new TurnCaptureSpec.PixelChangeProbe(220, 320, 80, 280, 100, 0.05D);
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(probeAction(
                        null, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE, validProbe)));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(probeAction(
                        region, TurnCaptureSpec.ResultMode.NO_IMAGE, validProbe)));

        TurnCaptureSpec bothPolicies = new TurnCaptureSpec(
                region,
                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                new TurnCaptureSpec.ClearPointerIfOverRegion(0, 120, 250, 0),
                validProbe);
        TurnAction mutuallyExclusive = TurnProtocolGoldenSupport.action(
                "probe-pointer-clear-mutual-exclusion",
                List.of(TurnProtocolGoldenSupport.captureStep(0, bothPolicies)));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(mutuallyExclusive));

        TurnAction extraStep = TurnProtocolGoldenSupport.action(
                "probe-must-be-single-step",
                List.of(
                        TurnProtocolGoldenSupport.captureStep(
                                0,
                                new TurnCaptureSpec(
                                        region,
                                        TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                                        null,
                                        validProbe)),
                        TurnProtocolGoldenSupport.waitStep(1, 1L)));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(extraStep));
    }

    @Test
    void capturePointerClearJsonAndGeometryRemainStrictAndClosed() throws IOException {
        TurnAction legacy = TurnProtocolGoldenSupport.readFixture("action-input-capture.json", TurnAction.class);
        TurnProtocolValidator.requireValid(legacy);
        assertNull(legacy.steps().get(3).capture().clearPointerIfOverRegion());

        TurnAction valid = pointerClearAction(
                new TurnRegion(200, 300, 100, 80),
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, 120, 250, 300));
        assertEquals(valid, TurnProtocolValidator.requireValid(valid));
        ObjectNode validJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(valid);
        TurnAction parsed = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(validJson), TurnAction.class);
        assertEquals(valid, TurnProtocolValidator.requireValid(parsed));

        ObjectNode explicitNull = validJson.deepCopy();
        captureNode(explicitNull).putNull("clearPointerIfOverRegion");
        TurnAction nullPolicy = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(explicitNull), TurnAction.class);
        assertNull(nullPolicy.steps().get(0).capture().clearPointerIfOverRegion());
        TurnProtocolValidator.requireValid(nullPolicy);

        for (String missing : List.of("paddingPx", "targetX", "targetY", "settleMs")) {
            ObjectNode missingKey = validJson.deepCopy();
            clearNode(missingKey).remove(missing);
            assertThrows(JsonProcessingException.class,
                    () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                            TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(missingKey),
                            TurnAction.class),
                    "missing pointer-clear key must fail: " + missing);
        }

        ObjectNode extraKey = validJson.deepCopy();
        clearNode(extraKey).put("unexpected", 1);
        assertThrows(JsonProcessingException.class,
                () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                        TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(extraKey),
                        TurnAction.class));

        for (String nullField : List.of("paddingPx", "targetX", "targetY", "settleMs")) {
            ObjectNode nullPrimitive = validJson.deepCopy();
            clearNode(nullPrimitive).putNull(nullField);
            assertThrows(JsonProcessingException.class,
                    () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                            TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(nullPrimitive),
                            TurnAction.class),
                    "null pointer-clear primitive must fail: " + nullField);
        }

        ObjectNode stringNumber = validJson.deepCopy();
        clearNode(stringNumber).put("paddingPx", "12");
        assertThrows(JsonProcessingException.class,
                () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                        TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(stringNumber),
                        TurnAction.class));
        ObjectNode floatingNumber = validJson.deepCopy();
        clearNode(floatingNumber).put("settleMs", 300.5D);
        assertThrows(JsonProcessingException.class,
                () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                        TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(floatingNumber),
                        TurnAction.class));

        TurnProtocolValidator.requireValid(pointerClearAction(
                new TurnRegion(200, 300, 100, 80),
                new TurnCaptureSpec.ClearPointerIfOverRegion(0, 120, 250, 0)));
        TurnProtocolValidator.requireValid(pointerClearAction(
                new TurnRegion(200, 300, 100, 80),
                new TurnCaptureSpec.ClearPointerIfOverRegion(128, 1_000, 1_000, 5_000)));

        for (TurnCaptureSpec.ClearPointerIfOverRegion invalid : List.of(
                new TurnCaptureSpec.ClearPointerIfOverRegion(-1, 120, 250, 300),
                new TurnCaptureSpec.ClearPointerIfOverRegion(129, 1_000, 1_000, 300),
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, 120, 250, -1),
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, 120, 250, 5_001))) {
            assertThrows(IllegalArgumentException.class,
                    () -> TurnProtocolValidator.requireValid(pointerClearAction(
                            new TurnRegion(200, 300, 100, 80), invalid)));
        }

        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(pointerClearAction(
                        null,
                        new TurnCaptureSpec.ClearPointerIfOverRegion(12, 120, 250, 300))));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(pointerClearAction(
                        new TurnRegion(200, 300, 100, 80),
                        new TurnCaptureSpec.ClearPointerIfOverRegion(12, 220, 320, 300))));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(pointerClearAction(
                        new TurnRegion(200, 300, 100, 80),
                        new TurnCaptureSpec.ClearPointerIfOverRegion(12, 188, 320, 300))));
    }

    @Test
    void invalidTypedUnionAndMoreThanOneRequestedFrameFailClosed() {
        TurnLocalServiceCall invalidCall = new TurnLocalServiceCall(
                TurnLocalOperation.BAG_USE_INCENSE,
                null,
                new TurnUiOperationArguments("must-not-be-present"),
                null,
                null);
        TurnAction invalidUnion = TurnProtocolGoldenSupport.action(
                "invalid-local-union",
                List.of(TurnProtocolGoldenSupport.localStep(0, invalidCall)));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(invalidUnion));

        TurnAction twoFrames = TurnProtocolGoldenSupport.action(
                "two-returned-frames",
                List.of(
                        TurnProtocolGoldenSupport.captureStep(0,
                                new TurnCaptureSpec(new TurnRegion(0, 0, 100, 100),
                                        TurnCaptureSpec.ResultMode.UPLOAD_IMAGE)),
                        TurnProtocolGoldenSupport.matchStep(1,
                                new TurnMatchSpec(new TurnRegion(0, 0, 50, 50), "dialog/example",
                                        TurnProtocolGoldenSupport.SHA_A, 0.9D, TurnMatchSpec.OnMatch.NONE,
                                        TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT_AND_IMAGE))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(twoFrames));

        TurnAction questDetailAndCaptureFrames = TurnProtocolGoldenSupport.action(
                "quest-detail-and-capture-frames",
                List.of(
                        TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                                TurnLocalOperation.QUEST_CAPTURE_DETAIL, null, null, null,
                                new TurnQuestOperationArguments("wuhuan", null))),
                        TurnProtocolGoldenSupport.captureStep(1,
                                new TurnCaptureSpec(new TurnRegion(0, 0, 100, 100),
                                        TurnCaptureSpec.ResultMode.UPLOAD_IMAGE))));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(questDetailAndCaptureFrames));
    }

    @Test
    void duplicateSkippedAndOutOfOrderStepIndicesFailClosed() {
        TurnAction duplicate = TurnProtocolGoldenSupport.action(
                "duplicate-step-index",
                List.of(TurnProtocolGoldenSupport.waitStep(0, 100L),
                        TurnProtocolGoldenSupport.waitStep(0, 200L)));
        TurnAction skipped = TurnProtocolGoldenSupport.action(
                "skipped-step-index",
                List.of(TurnProtocolGoldenSupport.waitStep(0, 100L),
                        TurnProtocolGoldenSupport.waitStep(2, 200L)));
        TurnAction outOfOrder = TurnProtocolGoldenSupport.action(
                "out-of-order-step-index",
                List.of(TurnProtocolGoldenSupport.waitStep(1, 100L),
                        TurnProtocolGoldenSupport.waitStep(0, 200L)));

        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(duplicate));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(skipped));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(outOfOrder));
    }

    @Test
    void actionIdFailedTailAndFrameShapeAreStrict() throws IOException {
        TurnOutcome completed = TurnProtocolGoldenSupport.readFixture("outcome-completed.json", TurnOutcome.class);
        TurnOutcome missingActionId = new TurnOutcome(
                completed.contractVersion(), " ", completed.window(), completed.status(), completed.failedStepIndex(),
                completed.code(), completed.message(), completed.stepResults(), completed.frame());
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(missingActionId));

        TurnFrameMetadata badHash = new TurnFrameMetadata(
                completed.frame().purpose(), completed.frame().contentType(), "not-a-sha", completed.frame().width(),
                completed.frame().height(), completed.frame().region(), completed.frame().sourceStepIndex());
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(withFrame(completed, badHash)));

        TurnFrameMetadata mismatchedDimensions = new TurnFrameMetadata(
                completed.frame().purpose(), completed.frame().contentType(), completed.frame().sha256(),
                completed.frame().width() - 1, completed.frame().height(), completed.frame().region(),
                completed.frame().sourceStepIndex());
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(withFrame(completed, mismatchedDimensions)));

        TurnOutcome failed = TurnProtocolGoldenSupport.readFixture("outcome-failed-with-frame.json", TurnOutcome.class);
        List<TurnStepResult> invalidTail = new ArrayList<>(failed.stepResults());
        TurnStepResult tail = invalidTail.get(2);
        invalidTail.set(2, new TurnStepResult(tail.index(), tail.type(), TurnStepResult.Status.COMPLETED,
                "SHOULD_NOT_RUN", tail.match(), tail.localResultJson()));
        TurnOutcome executedAfterFailure = new TurnOutcome(
                failed.contractVersion(), failed.actionId(), failed.window(), failed.status(), failed.failedStepIndex(),
                failed.code(), failed.message(), List.copyOf(invalidTail), failed.frame());
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(executedAfterFailure));
    }

    @Test
    void requestOutcomeAndResponseActionMustKeepExactDeviceWindowCorrelation() throws IOException {
        TurnRequest request = new TurnRequest(1, TurnProtocolGoldenSupport.window(false, false), 25_000L, null, null);
        TurnAction action = TurnProtocolGoldenSupport.readFixture("action-input-capture.json", TurnAction.class);
        TurnResponse validResponse = new TurnResponse(TurnResponse.Status.ACTION, action, null);
        assertEquals(validResponse, TurnProtocolValidator.requireValid(validResponse, request));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(new TurnResponse(TurnResponse.Status.ACTION, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(new TurnResponse(TurnResponse.Status.IDLE, action, null)));

        TurnAction wrongWindowAction = new TurnAction(
                action.contractVersion(), action.actionId(), action.deviceId(), "window-other", action.steps(),
                action.fullWindowFailureEvidence());
        TurnResponse wrongWindowResponse = new TurnResponse(TurnResponse.Status.ACTION, wrongWindowAction, null);
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(wrongWindowResponse, request));

        TurnOutcome completed = TurnProtocolGoldenSupport.readFixture("outcome-completed.json", TurnOutcome.class);
        TurnWindowMetadata otherWindow = new TurnWindowMetadata(
                completed.window().deviceId(), "window-other", completed.window().windowTitle(),
                completed.window().nativeHandle(), completed.window().processId(), completed.window().windowRect(),
                completed.window().pauseRequested(), completed.window().stopRequested());
        TurnOutcome wrongPreviousOutcome = new TurnOutcome(
                completed.contractVersion(), completed.actionId(), otherWindow, completed.status(),
                completed.failedStepIndex(), completed.code(), completed.message(), completed.stepResults(),
                completed.frame());
        TurnRequest wrongPrevious = new TurnRequest(1, request.window(), 25_000L, wrongPreviousOutcome, null);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(wrongPrevious));
    }

    private static TurnOutcome withFrame(TurnOutcome outcome, TurnFrameMetadata frame) {
        return new TurnOutcome(
                outcome.contractVersion(), outcome.actionId(), outcome.window(), outcome.status(),
                outcome.failedStepIndex(), outcome.code(), outcome.message(), outcome.stepResults(), frame);
    }

    private static TurnAction pointerClearAction(
            TurnRegion region,
            TurnCaptureSpec.ClearPointerIfOverRegion clear) {
        return TurnProtocolGoldenSupport.action(
                "pointer-clear-validator",
                List.of(TurnProtocolGoldenSupport.captureStep(
                        0,
                        new TurnCaptureSpec(region, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE, clear))));
    }

    private static TurnAction probeAction(TurnRegion region,
                                          TurnCaptureSpec.ResultMode resultMode,
                                          TurnCaptureSpec.PixelChangeProbe probe) {
        return TurnProtocolGoldenSupport.action(
                "pixel-change-probe-validator",
                List.of(TurnProtocolGoldenSupport.captureStep(
                        0,
                        new TurnCaptureSpec(region, resultMode, null, probe))));
    }

    private static TurnInputSpec inputFor(TurnInputAction action,
                                          Integer clickDelayMs,
                                          Integer queueHoldMs) {
        return switch (action) {
            case MOVE_MOUSE, CLICK_LEFT, CLICK_RIGHT, DOUBLE_CLICK_LEFT, DOUBLE_CLICK_RIGHT ->
                    new TurnInputSpec(140, 243, null, null, null, null, null,
                            clickDelayMs, queueHoldMs);
            case DRAG_LEFT -> new TurnInputSpec(140, 243, 141, 244, null, null, null,
                    clickDelayMs, queueHoldMs);
            case SCROLL -> new TurnInputSpec(140, 243, null, null, 1, null, null,
                    clickDelayMs, queueHoldMs);
            case KEY_TAP, KEY_DOWN, KEY_UP -> new TurnInputSpec(null, null, null, null, null, "Q", null,
                    clickDelayMs, queueHoldMs);
            case TEXT_INPUT -> new TurnInputSpec(null, null, null, null, null, null, "text",
                    clickDelayMs, queueHoldMs);
        };
    }

    private static ObjectNode captureNode(ObjectNode action) {
        return (ObjectNode) action.withArray("steps").get(0).get("capture");
    }

    private static ObjectNode clearNode(ObjectNode action) {
        return (ObjectNode) captureNode(action).get("clearPointerIfOverRegion");
    }

    private static ObjectNode probeNode(ObjectNode action) {
        return (ObjectNode) captureNode(action).get("pixelChangeProbe");
    }
}
