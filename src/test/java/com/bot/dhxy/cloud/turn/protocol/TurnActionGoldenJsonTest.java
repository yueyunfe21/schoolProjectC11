package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnActionGoldenJsonTest {

    @Test
    void canonicalActionFixtureRoundTripsWithOrderedTypedStepsAndOneFrame() throws IOException {
        TurnAction action = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "action-input-capture.json", TurnAction.class);
        TurnProtocolValidator.requireValid(action);

        assertEquals(1, action.contractVersion());
        assertEquals("action-input-capture-001", action.actionId());
        assertEquals("device-alpha", action.deviceId());
        assertEquals("window-2", action.windowId());
        assertEquals(List.of(TurnStepType.INPUT, TurnStepType.WAIT, TurnStepType.LOCAL_SERVICE,
                TurnStepType.CAPTURE), action.steps().stream().map(TurnStep::type).toList());
        assertEquals(List.of(0, 1, 2, 3), action.steps().stream().map(TurnStep::index).toList());

        TurnStep input = action.steps().get(0);
        assertEquals(TurnInputAction.CLICK_LEFT, input.inputAction());
        assertEquals(1420, input.input().x());
        assertEquals(736, input.input().y());
        assertNull(input.input().clickDelayMs(), "legacy input JSON must keep click delay absent");
        assertNull(input.input().queueHoldMs(), "legacy input JSON must keep queue hold absent");

        TurnLocalServiceCall local = action.steps().get(2).localService();
        assertEquals(TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT, local.operation());
        assertEquals("golden-action", local.ui().source());
        assertNull(local.bag());
        assertNull(local.giveItem());
        assertNull(local.quest());

        TurnCaptureSpec capture = action.steps().get(3).capture();
        assertEquals(TurnCaptureSpec.ResultMode.UPLOAD_IMAGE, capture.resultMode());
        assertEquals(new TurnRegion(1080, 420, 700, 500), capture.region());
        assertNull(capture.clearPointerIfOverRegion(), "legacy capture JSON must keep pointer-clear absent");
        assertNull(capture.pixelChangeProbe(), "legacy capture JSON must keep pixel-change probe absent");
    }

    @Test
    void pointerClearCaptureRoundTripsWithOnlyTheFourExactMechanicalFields() throws IOException {
        TurnCaptureSpec.ClearPointerIfOverRegion clear =
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, 120, 250, 300);
        TurnAction action = TurnProtocolGoldenSupport.action(
                "capture-pointer-clear-round-trip",
                List.of(TurnProtocolGoldenSupport.captureStep(
                        0,
                        new TurnCaptureSpec(
                                new TurnRegion(200, 300, 100, 80),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                                clear))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(clear, roundTripped.steps().get(0).capture().clearPointerIfOverRegion());
        ObjectNode actionJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(roundTripped);
        ObjectNode captureJson = (ObjectNode) actionJson.withArray("steps").get(0).get("capture");
        assertEquals(3, captureJson.size());
        assertTrue(captureJson.has("region"));
        assertTrue(captureJson.has("resultMode"));
        assertTrue(captureJson.has("clearPointerIfOverRegion"));
        ObjectNode clearJson = (ObjectNode) captureJson.get("clearPointerIfOverRegion");
        assertEquals(4, clearJson.size());
        assertEquals(12, clearJson.get("paddingPx").intValue());
        assertEquals(120, clearJson.get("targetX").intValue());
        assertEquals(250, clearJson.get("targetY").intValue());
        assertEquals(300, clearJson.get("settleMs").intValue());
    }

    @Test
    void moveMouseRoundTripsWithOnlyItsScreenAbsolutePoint() throws IOException {
        TurnAction action = TurnProtocolGoldenSupport.action(
                "move-mouse-round-trip",
                List.of(TurnProtocolGoldenSupport.inputStep(
                        0,
                        TurnInputAction.MOVE_MOUSE,
                        new TurnInputSpec(1420, 736, null, null, null, null, null))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
        TurnStep step = roundTripped.steps().get(0);
        assertEquals(TurnInputAction.MOVE_MOUSE, step.inputAction());
        assertEquals(1420, step.input().x());
        assertEquals(736, step.input().y());
        assertNull(step.input().endX());
        assertNull(step.input().endY());
        assertNull(step.input().scrollDelta());
        assertNull(step.input().key());
        assertNull(step.input().text());
    }

    @Test
    void clickTimingAndPixelChangeFixturesRoundTripWithOnlyFrozenMechanicalFields() throws IOException {
        TurnAction click = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "action-input-click-timing.json", TurnAction.class);
        TurnProtocolValidator.requireValid(click);
        TurnInputSpec clickInput = click.steps().get(0).input();
        assertEquals(150, clickInput.clickDelayMs());
        assertEquals(500, clickInput.queueHoldMs());

        TurnAction probeAction = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "action-capture-pixel-change.json", TurnAction.class);
        TurnProtocolValidator.requireValid(probeAction);
        TurnCaptureSpec probeCapture = probeAction.steps().get(0).capture();
        assertNull(probeCapture.clearPointerIfOverRegion());
        TurnCaptureSpec.PixelChangeProbe probe = probeCapture.pixelChangeProbe();
        assertEquals(140, probe.targetX());
        assertEquals(243, probe.targetY());
        assertEquals(80, probe.ctrlDownSettleMs());
        assertEquals(280, probe.afterMoveSettleMs());
        assertEquals(100, probe.ctrlUpSettleMs());
        assertEquals(0.05D, probe.differenceRatioThreshold());

        TurnOutcome outcome = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "outcome-capture-pixel-change.json", TurnOutcome.class);
        TurnProtocolValidator.requireValid(outcome);
        assertEquals("PIXELS_CHANGED", outcome.stepResults().get(0).code());
        assertEquals(new TurnRegion(139, 242, 2, 2), outcome.frame().region());
        assertEquals(0, outcome.frame().sourceStepIndex());
    }

    @Test
    void metricLocalStepRoundTripsWithOnlyTheMetricSlotAndVerbatimCaseDir() throws IOException {
        TurnMetricEventPayload payload = new TurnMetricEventPayload(
                "xiuluo_v2", "修罗", "window-2", "MEMBER", "0x5151",
                null, null, null, null, null, "watchdog timeout", null,
                "D:\\cloud\\cases\\2026-07-18\\case-golden", "PRE_COMBAT_TIMEOUT",
                "WAIT_TRACKER", 8, null);
        TurnAction action = TurnProtocolGoldenSupport.action(
                "metric-local-step-round-trip",
                List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                        TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE,
                        null, null, null, null, null, payload))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(payload, roundTripped.steps().get(0).localService().metric());
        ObjectNode actionJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(roundTripped);
        ObjectNode localJson = (ObjectNode) actionJson.withArray("steps").get(0).get("localService");
        assertTrue(localJson.has("operation"));
        assertTrue(localJson.has("metric"));
        assertTrue(localJson.get("bag").isNull(), "the metric slot never rides with another argument group");
        ObjectNode metricJson = (ObjectNode) localJson.get("metric");
        assertEquals("D:\\cloud\\cases\\2026-07-18\\case-golden", metricJson.get("caseDir").textValue(),
                "the Cloud filesystem locator serializes verbatim");
        assertEquals("0x5151", metricJson.get("nativeWindowHandle").textValue());
        assertEquals(8, metricJson.get("round").intValue());
    }

    @Test
    void queueOwningBagLocalStepRoundTripsWithOnlyTheBagSlot() throws IOException {
        TurnBagOperationArguments bag = new TurnBagOperationArguments(
                null, "wuhuan/shoe.png", 3, null, "wuhuan-v2:prepare-supplies");
        TurnAction action = TurnProtocolGoldenSupport.action(
                "bag-supply-check-round-trip",
                List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                        TurnLocalOperation.BAG_FIVERING_SUPPLY_CHECK, bag, null, null, null))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(bag, roundTripped.steps().get(0).localService().bag());
        ObjectNode actionJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.valueToTree(roundTripped);
        ObjectNode localJson = (ObjectNode) actionJson.withArray("steps").get(0).get("localService");
        assertTrue(localJson.has("operation"));
        assertTrue(localJson.has("bag"));
        assertTrue(localJson.get("metric").isNull(), "the bag slot never rides with a metric payload");
        ObjectNode bagJson = (ObjectNode) localJson.get("bag");
        assertEquals(3, bagJson.get("maxBagIndex").intValue(), "requiredCount rides the maxBagIndex slot");
        assertEquals("wuhuan/shoe.png", bagJson.get("targetItemTemplate").textValue());
        assertTrue(bagJson.get("intent").isNull(), "a queue-owning bag op carries no return-item intent");
    }

    @Test
    void missingActionIdAndMixedStepUnionFailClosed() throws IOException {
        TurnAction action = TurnProtocolGoldenSupport.readFixture("action-input-capture.json", TurnAction.class);
        TurnAction missingId = new TurnAction(action.contractVersion(), null, action.deviceId(), action.windowId(),
                action.steps(), action.fullWindowFailureEvidence());
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(missingId));

        TurnStep first = action.steps().get(0);
        TurnStep mixed = new TurnStep(first.index(), first.type(), first.inputAction(), first.input(), null,
                new TurnCaptureSpec(new TurnRegion(0, 0, 10, 10), TurnCaptureSpec.ResultMode.NO_IMAGE), null, null);
        List<TurnStep> mixedSteps = new ArrayList<>(action.steps());
        mixedSteps.set(0, mixed);
        TurnAction invalidUnion = new TurnAction(action.contractVersion(), action.actionId(), action.deviceId(),
                action.windowId(), List.copyOf(mixedSteps), action.fullWindowFailureEvidence());
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(invalidUnion));
    }
}
