package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnProtocolValidatorContractTest {

    @Test
    void continuationKindStageAndDirectiveShapeRemainClosed() {
        TurnWindowMetadata window = TurnProtocolGoldenSupport.window(false, false);
        TurnContinuationRequest tick = new TurnContinuationRequest(
                "action-continuation", 0, TurnContinuationRequest.Kind.FIVERING_INCENSE,
                TurnContinuationRequest.Stage.TICK, null, null);
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(
                new TurnRequest(1, window, 0L, null, null, tick)));

        TurnContinuationRequest wrongStage = new TurnContinuationRequest(
                "action-continuation", 0, TurnContinuationRequest.Kind.FIVERING_INCENSE,
                TurnContinuationRequest.Stage.STATUS_IMAGE, null, null);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                new TurnRequest(1, window, 0L, null, null, wrongStage)));
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(new TurnResponse(
                TurnResponse.Status.CONTINUATION, null, null,
                new TurnContinuationDecision(
                        TurnContinuationDecision.Directive.USE_INCENSE, "decision-1", "use incense"))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(new TurnResponse(
                TurnResponse.Status.CONTINUATION, null, null,
                new TurnContinuationDecision(
                        TurnContinuationDecision.Directive.USE_INCENSE, "decision-1", "use incense", 420, 360))));
    }

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
    void taskStartWindowAuthorityRequiresPresentValidAndConsistentRoleTeamStartupFacts() {
        // Valid: a solo, leader-absent window (no local team) with a role and NORMAL startup.
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow("LEADER", null, null, false, false, "NORMAL"))));
        // Valid: a present-leader team member window with session, leader id, support member, non-NORMAL startup.
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(taskStart(
                authorityWindow("MEMBER", "team-1", "window-leader", true, true, "AFTER_COMBAT_EXIT_STARTUP"))));

        // Missing role or startup mode fails closed.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow(null, null, null, false, false, "NORMAL"))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow("LEADER", null, null, false, false, null))));
        // An unknown startupMode name (not an existing TaskStartupMode) fails closed.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow("LEADER", null, null, false, false, "TURBO"))));
        // A missing (null) boxed authority boolean is rejected, distinct from a legitimate false.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow("LEADER", null, null, null, false, "NORMAL"))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                taskStart(authorityWindow("LEADER", null, null, false, null, "NORMAL"))));

        // Team-fact contradictions against the baseline invariants fail closed.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(taskStart(
                authorityWindow("MEMBER", "team-1", "window-leader", false, true, "NORMAL")))); // support w/o leader
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(taskStart(
                authorityWindow("LEADER", "team-1", null, false, false, "NORMAL")))); // session w/o leader
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(taskStart(
                authorityWindow("LEADER", null, "window-leader", false, false, "NORMAL")))); // leader id w/o leader
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(taskStart(
                authorityWindow("LEADER", null, "window-leader", true, false, "NORMAL")))); // leader present w/o session
    }

    private static TurnWindowMetadata authorityWindow(String windowRole, String localTeamSessionKey,
                                                      String localLeaderWindowId, Boolean localLeaderPresent,
                                                      Boolean localSupportMember, String startupMode) {
        return new TurnWindowMetadata(
                "device-alpha", "window-2", "Classic Client - Alpha", "0x000000000001A2B3", 4242L,
                new TurnWindowRect(120, 80, 1280, 720), false, false, null,
                windowRole, localTeamSessionKey, localLeaderWindowId, localLeaderPresent, localSupportMember,
                startupMode);
    }

    private static TurnRequest taskStart(TurnWindowMetadata window) {
        return new TurnRequest(1, window, 25_000L, null,
                new TurnTaskStartRequest("start-authority-001", List.of(TurnTaskCode.WUHUAN_V2),
                        List.of(1), TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE));
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
    void autoCombatPanelDragMarkerIsClosedToDragLeft() {
        TurnInputSpec markedDrag = new TurnInputSpec(
                140, 243, 141, 244, null, null, null,
                null, null, null, null, true);
        TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                "auto-panel-drag",
                List.of(TurnProtocolGoldenSupport.inputStep(0, TurnInputAction.DRAG_LEFT, markedDrag))));

        TurnInputSpec markedClick = new TurnInputSpec(
                140, 243, null, null, null, null, null,
                null, null, null, null, true);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action(
                        "auto-panel-marker-on-click",
                        List.of(TurnProtocolGoldenSupport.inputStep(
                                0, TurnInputAction.CLICK_LEFT, markedClick)))));
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
    void findAndUseTaskPageBagIntentRejectsBlankTemplateAndNonCanonicalShape() {
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                findAndUseTaskPageAction("find-and-use-blank-template", "  ", -1, null)));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                findAndUseTaskPageAction("find-and-use-non-negative-index", "bag/item.png", 0, null)));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                findAndUseTaskPageAction("find-and-use-cached-point", "bag/item.png", -1,
                        new TurnReturnItemCachePoint("cache.png", 10, 20, 5L, "src"))));
    }

    private static TurnAction findAndUseTaskPageAction(String actionId, String template, Integer maxBagIndex,
                                                       TurnReturnItemCachePoint cachedPoint) {
        return TurnProtocolGoldenSupport.action(actionId, List.of(TurnProtocolGoldenSupport.localStep(0,
                new TurnLocalServiceCall(TurnLocalOperation.BAG_RETURN_ITEM,
                        new TurnBagOperationArguments(
                                TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                                template, maxBagIndex, cachedPoint, "golden"),
                        null, null, null))));
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

    @Test
    void wholeTaskRuntimeOperationsEnforcePerOperationPayload() {
        // Pathing register: needs a pathing intent with a nonblank intentId/source/type.
        TurnPathingIntent intent = new TurnPathingIntent("nav", "intent-1", "长安", 10, 20, 5, "TARGETED");
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_REGISTER, withPathingIntent("reg", intent));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_REGISTER, sourceOnly("reg"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_REGISTER,
                withPathingIntent("reg", new TurnPathingIntent("nav", " ", "长安", null, null, 5, "TARGETED")));

        // Clear-intent: needs an exact nonblank intentId (mismatch-no-op is a runtime, not wire, rule).
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_INTENT, withIntentId("c", "intent-1"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_INTENT, sourceOnly("c"));

        // Clear-source-prefix: needs a nonblank source prefix.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX,
                withSourcePrefix("p", "wubei:tracker-green-click:"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX, sourceOnly("p"));

        // Timer start / target-map-gate: need a task code.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_START, withTaskCode("t", "wubei"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_START, sourceOnly("t"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_START, withTaskCode("g", "wubei"));

        // Timer pause: needs a nonnegative blockedMs.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE, withBlockedMs("pz", 500L));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE, withBlockedMs("pz", -1L));

        // Dialog-interest update: needs a task code and a nonempty operations list.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_UPDATE,
                interestArgs("di", "wubei", List.of("WUBEI_ENTER_BATTLE")));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_UPDATE,
                interestArgs("di", "wubei", List.of()));

        // Progress: needs nonnegative completedRuns and a totalRuns.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PROGRESS_UPDATE, progressArgs("pr", 1, 5));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PROGRESS_UPDATE, sourceOnly("pr"));

        // Source-only operations accept a source and reject a blank one.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR, sourceOnly("pc"));
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_READ, sourceOnly("pr"));
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_RECOVERY_RESET, sourceOnly("legacy-reset"));
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_RECOVERY_RESET,
                new Wtb("exact-reset").recoveryIdentity("task-run-1", 3, "attempt-3").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_RECOVERY_RESET,
                new Wtb("partial-reset").recoveryIdentity("task-run-1", null, "attempt-3").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_RECOVERY_RESET,
                new Wtb("invalid-round-reset").recoveryIdentity("task-run-1", 0, "attempt-3").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR, sourceOnly(" "));

        // Pathing late target-map upgrade: needs a nonblank intentId and targetMapName together.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                new Wtb("u").intentId("intent-1").targetMapName("长安").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP, sourceOnly("u"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                new Wtb("u").intentId("intent-1").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                new Wtb("u").targetMapName("长安").build());

        // Dialog runtime read: source-only (unbounded) is valid; an optional maxAge must be nonnegative.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_RUNTIME_READ, sourceOnly("d"));
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_RUNTIME_READ,
                new Wtb("d").dialogSnapshotMaxAgeMs(1500L).build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_RUNTIME_READ,
                new Wtb("d").dialogSnapshotMaxAgeMs(-1L).build());
        // Exactly-one arg union: a whole-task op must not carry another argument group.
        TurnLocalServiceCall mixed = new TurnLocalServiceCall(
                TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR,
                null, new TurnUiOperationArguments("must-not-be-present"), null, null, sourceOnly("f"));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action("wt-mixed",
                        List.of(TurnProtocolGoldenSupport.localStep(0, mixed)))));

        // A whole-task operation with no whole-task arguments at all is rejected.
        TurnLocalServiceCall missing = new TurnLocalServiceCall(
                TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action("wt-missing",
                        List.of(TurnProtocolGoldenSupport.localStep(0, missing)))));
    }

    @Test
    void wholeTaskRuntimeOperationsRejectFieldsOutsideTheirOwnPayload() {
        TurnPathingIntent intent = new TurnPathingIntent("nav", "intent-1", "长安", 1, 2, 5, "TARGETED");

        // A source-only operation must not smuggle another op's field.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR,
                new Wtb("a").interestOperations(List.of("WUBEI_ENTER_BATTLE")).build());

        // A clear operation with its required field plus an extra known field is rejected.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_INTENT,
                new Wtb("c").intentId("intent-1").completedRuns(1).build());

        // The dialog runtime read allows only its optional dialogSnapshotMaxAgeMs; another op's field is rejected.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_DIALOG_RUNTIME_READ,
                new Wtb("d").dialogSnapshotMaxAgeMs(1500L).intentId("intent-1").build());

        // The unconditional pathing clear is source-only and must not smuggle an intentId or prefix.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR,
                new Wtb("pc").intentId("intent-1").build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_READ,
                new Wtb("pr").intentId("intent-1").build());

        // A multi-field operation with an out-of-payload extra is rejected.
        // The pathing target-map upgrade owns only intentId+targetMapName; any extra field is rejected.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                new Wtb("u").intentId("intent-1").targetMapName("长安").completedRuns(1).build());
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PROGRESS_UPDATE,
                new Wtb("p").completedRuns(1).totalRuns(5).taskCode("wubei").build());

        // The exactly-correct payloads still validate.
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PATHING_CLEAR_INTENT,
                new Wtb("c").intentId("intent-1").build());
    }

    @Test
    void queueOwningBagOperationsValidateExactPerOperationShape() {
        // Valid shapes.
        requireValidBag(TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK,
                new TurnBagOperationArguments(null, "wuhuan/shoe.png", 3, null, "s"));
        requireValidBag(TurnLocalOperation.BAG_FIND_AND_USE_FROM_BACK,
                new TurnBagOperationArguments(null, "bag/probe.png", 5, null, "s"));
        requireValidBag(TurnLocalOperation.BAG_FIND_ITEM_PAGE_INDEX,
                new TurnBagOperationArguments(null, "bag/shoe.png", null, null, "s"));

        // SUPPLY_CHECK: requiredCount (maxBagIndex slot) must be positive; template/source required;
        // no intent or cached point.
        assertRejectedBag(TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK,
                new TurnBagOperationArguments(null, "wuhuan/shoe.png", 0, null, "s"));
        assertRejectedBag(TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK,
                new TurnBagOperationArguments(null, null, 3, null, "s"));
        assertRejectedBag(TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK,
                new TurnBagOperationArguments(
                        TurnBagOperationArguments.ReturnItemIntent.PRESCAN_TASK_PAGE,
                        "wuhuan/shoe.png", 3, null, "s"));

        // FIND_AND_USE_FROM_BACK: maxBagIndex must be positive; template/source required.
        assertRejectedBag(TurnLocalOperation.BAG_FIND_AND_USE_FROM_BACK,
                new TurnBagOperationArguments(null, "bag/probe.png", 0, null, "s"));
        assertRejectedBag(TurnLocalOperation.BAG_FIND_AND_USE_FROM_BACK,
                new TurnBagOperationArguments(null, "bag/probe.png", null, null, "s"));

        // FIND_ITEM_PAGE_INDEX: only template + source; a maxBagIndex is rejected.
        assertRejectedBag(TurnLocalOperation.BAG_FIND_ITEM_PAGE_INDEX,
                new TurnBagOperationArguments(null, "bag/shoe.png", 2, null, "s"));
        assertRejectedBag(TurnLocalOperation.BAG_FIND_ITEM_PAGE_INDEX,
                new TurnBagOperationArguments(null, null, null, null, "s"));

        // A queue-owning bag op must not carry another argument group.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action("bag-mixed",
                        List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                                TurnLocalOperation.BAG_FIND_ITEM_PAGE_INDEX,
                                new TurnBagOperationArguments(null, "bag/shoe.png", null, null, "s"),
                                new TurnUiOperationArguments("x"), null, null))))));
    }

    private static void requireValidBag(TurnLocalOperation operation, TurnBagOperationArguments bag) {
        TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                "bag-ok-" + operation,
                List.of(TurnProtocolGoldenSupport.localStep(0,
                        new TurnLocalServiceCall(operation, bag, null, null, null)))));
    }

    private static void assertRejectedBag(TurnLocalOperation operation, TurnBagOperationArguments bag) {
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action(
                        "bag-bad-" + operation,
                        List.of(TurnProtocolGoldenSupport.localStep(0,
                                new TurnLocalServiceCall(operation, bag, null, null, null))))));
    }

    @Test
    void metricOperationsValidateIdentityAndExactPerOperationShape() {
        // The exactly-correct payload for each of the three metric operations validates.
        requireValidMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, startedPayload());
        requireValidMetric(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, finishedPayload());
        requireValidMetric(TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE, failureCasePayload());

        // Every persisted identity field is mandatory — the local authority never synthesizes it.
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                null, "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", null, null, "m", null, null, null, null, null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", " ",
                "round-7", 7, "普通怪", null, null, "m", null, null, null, null, null, null));

        // Per-operation required fields are enforced.
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                null, 7, "普通怪", null, null, "m", null, null, null, null, null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", null, "SUCCESS", "m", 100L, null, null, null, null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", "SUCCESS", "SUCCESS", "m", -1L, null, null, null, null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE, new TurnMetricEventPayload(
                "xiuluo_v2", "修罗", "window-2", "MEMBER", "0x5151",
                null, null, null, null, null, "m", null, null, "REASON", "PHASE", 8, null));

        // The failure-only phase field never rides a round operation, and the FINISHED status is a
        // closed set at the wire boundary — an unknown value is rejected before any adapter runs.
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", null, null, "m", null, null, null, "SMUGGLED_PHASE", null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", "SUCCESS", "SUCCESS", "m", 100L, null, null, "SMUGGLED_PHASE", null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", "NOT_A_STATUS", "SUCCESS", "m", 100L, null, null, null, null, null));

        // Cross-operation field smuggling is rejected in both directions.
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", null, null, "m", null,
                "D:\\cloud\\cases\\case-8", null, null, null, null));
        assertRejectedMetric(TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE, new TurnMetricEventPayload(
                "xiuluo_v2", "修罗", "window-2", "MEMBER", "0x5151",
                "round-8", null, null, null, null, "m", null,
                "D:\\cloud\\cases\\case-8", "REASON", "PHASE", 8, null));

        // A metric operation without metric arguments, and a non-metric operation carrying metric
        // arguments, are both rejected.
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action("metric-missing",
                        List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                                TurnLocalOperation.METRIC_RECORD_ROUND_STARTED,
                                null, null, null, null, null, null))))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action("metric-smuggled",
                        List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                                TurnLocalOperation.UI_CLEAN_ALL,
                                null, null, null, null, null, startedPayload()))))));
    }

    private static TurnMetricEventPayload startedPayload() {
        return new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", null, null, "轮次开始", null,
                null, null, null, null, Map.of("sourcePhase", "ACCEPT"));
    }

    private static TurnMetricEventPayload finishedPayload() {
        return new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", "SUCCESS", "SUCCESS", "轮次完成", 1234L,
                null, null, null, null, Map.of("sourcePhase", "COMBAT"));
    }

    private static TurnMetricEventPayload failureCasePayload() {
        return new TurnMetricEventPayload(
                "xiuluo_v2", "修罗", "window-2", "MEMBER", "0x5151",
                null, null, null, null, null, "watchdog timeout", null,
                "D:\\cloud\\cases\\2026-07-18\\case-8", "PRE_COMBAT_TIMEOUT", "WAIT_TRACKER", 8, null);
    }

    private static void requireValidMetric(TurnLocalOperation operation, TurnMetricEventPayload payload) {
        TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                "metric-ok-" + operation,
                List.of(TurnProtocolGoldenSupport.localStep(0,
                        new TurnLocalServiceCall(operation, null, null, null, null, null, payload)))));
    }

    private static void assertRejectedMetric(TurnLocalOperation operation, TurnMetricEventPayload payload) {
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action(
                        "metric-bad-" + operation,
                        List.of(TurnProtocolGoldenSupport.localStep(0,
                                new TurnLocalServiceCall(operation, null, null, null, null, null, payload))))));
    }

    /** Compact builder so mixed-known-field negatives stay readable. */
    private static final class Wtb {
        private final String source;
        private TurnPathingIntent pathingIntent;
        private String intentId;
        private String targetMapName;
        private Long confirmTimeoutMs;
        private Long blockedMs;
        private String taskCode;
        private List<String> interestOperations;
        private Integer completedRuns;
        private Integer totalRuns;
        private Long dialogSnapshotMaxAgeMs;
        private String recoveryTaskRunId;
        private Integer recoveryRound;
        private String recoveryAttemptId;

        private Wtb(String source) {
            this.source = source;
        }

        private Wtb pathingIntent(TurnPathingIntent v) { this.pathingIntent = v; return this; }
        private Wtb intentId(String v) { this.intentId = v; return this; }
        private Wtb targetMapName(String v) { this.targetMapName = v; return this; }
        private Wtb confirmTimeoutMs(Long v) { this.confirmTimeoutMs = v; return this; }
        private Wtb blockedMs(Long v) { this.blockedMs = v; return this; }
        private Wtb taskCode(String v) { this.taskCode = v; return this; }
        private Wtb interestOperations(List<String> v) { this.interestOperations = v; return this; }
        private Wtb completedRuns(Integer v) { this.completedRuns = v; return this; }
        private Wtb totalRuns(Integer v) { this.totalRuns = v; return this; }
        private Wtb dialogSnapshotMaxAgeMs(Long v) { this.dialogSnapshotMaxAgeMs = v; return this; }
        private Wtb recoveryIdentity(String taskRunId, Integer round, String attemptId) {
            this.recoveryTaskRunId = taskRunId;
            this.recoveryRound = round;
            this.recoveryAttemptId = attemptId;
            return this;
        }

        private TurnWholeTaskRuntimeArguments build() {
            return new TurnWholeTaskRuntimeArguments(
                    source, pathingIntent, intentId, null, null, null, null, null,
                    targetMapName, null, null, null, confirmTimeoutMs, taskCode, null,
                    blockedMs, interestOperations, null, null, completedRuns, totalRuns,
                    dialogSnapshotMaxAgeMs, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, recoveryTaskRunId, recoveryRound, recoveryAttemptId);
        }
    }

    private static void requireValidWholeTask(TurnLocalOperation operation, TurnWholeTaskRuntimeArguments args) {
        TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(
                "wt-ok-" + operation,
                List.of(TurnProtocolGoldenSupport.localStep(0,
                        new TurnLocalServiceCall(operation, null, null, null, null, args)))));
    }

    private static void assertRejectedWholeTask(TurnLocalOperation operation, TurnWholeTaskRuntimeArguments args) {
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action(
                        "wt-bad-" + operation,
                        List.of(TurnProtocolGoldenSupport.localStep(0,
                                new TurnLocalServiceCall(operation, null, null, null, null, args))))));
    }

    private static TurnWholeTaskRuntimeArguments sourceOnly(String source) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments transferChoiceArgs(String source, TurnPendingTransferChoice tc) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, tc, null, null);
    }

    private static TurnWholeTaskRuntimeArguments routeOutcomeReplaceArgs(
            String source, TurnPendingRouteOutcome ro, String reason) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, ro, reason);
    }

    @Test
    void pendingTransferChoiceUpdateEnforcesTransferChoicePayload() {
        TurnPendingTransferChoice tc = new TurnPendingTransferChoice(
                "长安", 1, 2, "宝象国", 3, 4, "去宝象国", "src", 100L);
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE,
                transferChoiceArgs("tc", tc));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE, sourceOnly("tc"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE,
                withIntentId("tc", "x"));
    }

    @Test
    void observerAtomicGateAndSettlementConsumesHaveClosedPayloads() {
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST,
                interestArgs("gate", "wubei", List.of("WUBEI_ENTER_BATTLE")));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST,
                withTaskCode("gate", "wubei"));
        TurnWholeTaskRuntimeArguments fence = new TurnWholeTaskRuntimeArguments(
                "settle", null, "intent-1", "route:one", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME, fence);
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME, fence);
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                withIntentId("settle", "intent-1"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME,
                withSourcePrefix("settle", "route:one"));
    }

    @Test
    void npcArrivalFifoRequiresDedicatedDistinctRunIdentitiesAndClosedPayload() {
        TurnNpcArrivalFrameFifoSpec valid = arrivalFifoSpec(
                "remote-turn-x", "remote-turn-x:0:XIULUO_V2",
                0, 0, 1024, 768);
        requireValidWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(valid, null));
        assertEquals("remote-turn-x", valid.observationRunId());
        assertEquals("remote-turn-x:0:XIULUO_V2", valid.businessTaskRunId());

        assertRejectedWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(arrivalFifoSpec(
                        "", "remote-turn-x:0:XIULUO_V2", 0, 0, 1024, 768), null));
        assertRejectedWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(arrivalFifoSpec(
                        "remote-turn-x", "", 0, 0, 1024, 768), null));
        assertRejectedWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(arrivalFifoSpec(
                        "remote-turn-x", "remote-turn-x:0:XIULUO_V2",
                        1000, 0, 40, 768), null));
        assertRejectedWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(null, null));
        assertRejectedWholeTask(
                TurnLocalOperation.WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                arrivalFifoArgs(valid, "灵兽村"));
    }

    @Test
    void pendingRouteOutcomeReadIsSourceOnly() {
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ, sourceOnly("ro-read"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ,
                withIntentId("ro-read", "x"));
    }

    @Test
    void pendingRouteOutcomeReplaceEnforcesRouteOutcomePayload() {
        TurnPendingRouteOutcome ro = new TurnPendingRouteOutcome(
                "长安", "宝象国", "YELLOW_DESTINATION_MINI_MAP", 3, 4, "宝象国", "src", false, "rd-1", "int-1", 100L);
        requireValidWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", ro, "second-navigation"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE, sourceOnly("ro"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                transferChoiceArgs("ro", new TurnPendingTransferChoice(
                        null, null, null, null, null, null, null, "s", 0L)));
        // P-PROTO Review #1 P1-1: a nonblank routeOutcomeReplacementReason is required.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", ro, null));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", ro, "   "));
        // P-PROTO Review #2: routeMode must equal the sole DHXY wire value YELLOW_DESTINATION_MINI_MAP;
        // blank, the Cloud legacy name, and unknown names are all rejected.
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", new TurnPendingRouteOutcome(
                        "长安", "宝象国", "   ", 3, 4, "宝象国", "src", false, "rd-1", "int-1", 100L),
                        "second-navigation"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", new TurnPendingRouteOutcome(
                        "长安", "宝象国", "LEGACY_GREEN_LINK", 3, 4, "宝象国", "src", false, "rd-1", "int-1", 100L),
                        "second-navigation"));
        assertRejectedWholeTask(TurnLocalOperation.WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                routeOutcomeReplaceArgs("ro", new TurnPendingRouteOutcome(
                        "长安", "宝象国", "UNRECOGNIZED_ROUTE_MODE", 3, 4, "宝象国", "src", false, "rd-1", "int-1", 100L),
                        "second-navigation"));
    }

    private static TurnWholeTaskRuntimeArguments withPathingIntent(String source, TurnPathingIntent intent) {
        return new TurnWholeTaskRuntimeArguments(source, intent, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private static TurnNpcArrivalFrameFifoSpec arrivalFifoSpec(
            String observationRunId,
            String businessTaskRunId,
            int left,
            int top,
            int width,
            int height) {
        return new TurnNpcArrivalFrameFifoSpec(
                "tenant-1", "device-1", "window-7", "12345",
                observationRunId, businessTaskRunId,
                left, top, width, height,
                List.of("images/template/xiuluo/enter_battle.png"),
                null, false, true, false);
    }

    private static TurnWholeTaskRuntimeArguments arrivalFifoArgs(
            TurnNpcArrivalFrameFifoSpec spec,
            String shadowTargetMapName) {
        return new TurnWholeTaskRuntimeArguments(
                "arrival-fifo-test",
                null,
                "intent-1",
                null,
                null,
                null,
                null,
                null,
                shadowTargetMapName,
                null,
                null,
                null,
                null,
                "XIULUO_V2",
                "降魔侍卫",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                spec);
    }

    private static TurnWholeTaskRuntimeArguments withIntentId(String source, String intentId) {
        return new TurnWholeTaskRuntimeArguments(source, null, intentId, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments withSourcePrefix(String source, String prefix) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, prefix, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments nearArgs(String source, Integer cx, Integer cy,
                                                          Integer tx, Integer ty, Integer tol) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, cx, cy, null, tx, ty, tol,
                null, null, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments confirmArgs(String source, String targetMap, Long timeoutMs) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, targetMap,
                null, null, null, timeoutMs, null, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments withTaskCode(String source, String taskCode) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null,
                null, null, null, null, null, taskCode, null, null, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments withBlockedMs(String source, Long blockedMs) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, blockedMs, null, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments interestArgs(String source, String taskCode, List<String> ops) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, taskCode, null, null, ops, null, null, null, null);
    }

    private static TurnWholeTaskRuntimeArguments progressArgs(String source, Integer completed, Integer total) {
        return new TurnWholeTaskRuntimeArguments(source, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, completed, total);
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
