package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XinshouCombatLocalMechanicsTest {

    @Test
    void ordinaryCombatPressesAltATwiceInOneFocusedTransaction() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.pressOrdinaryAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(List.of("ALT_A", "ALT_A"), port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
        assertEquals(1, port.executionCount);
    }

    @Test
    void ordinaryCombatSecondAltAFailureFailsTheSingleTransaction() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.failAltAAtCall = 2;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.pressOrdinaryAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.INPUT_FAILED, result.status());
        assertEquals(List.of("ALT_A", "ALT_A"), port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
        assertEquals(1, port.executionCount);
    }

    @Test
    void ordinaryCombatDoesNotPressAltAWhenTheFreshCombatGateIsAbsent() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.combatVisibility = XinshouCombatLocalMechanics.CombatVisibility.ABSENT;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result = mechanics.pressOrdinaryAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.COMBAT_NOT_VISIBLE, result.status());
        assertEquals(List.of(), port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
    }

    @Test
    void captureUsesAltBWaitAbsentProbeThenOneClick() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.ABSENT;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(XinshouCombatLocalMechanics.PanelVisibility.ABSENT, result.observedPanel());
        assertEquals(
                List.of("CONTAINS:1246,777", "ALT_B", "WAIT:1000", "PROBE", "CLICK:1246,777"),
                port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
    }

    @Test
    void captureVisibleIsTerminalAndDoesNotClick() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.VISIBLE;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(
                XinshouCombatLocalMechanics.Status.PANEL_STILL_VISIBLE,
                result.status());
        assertEquals(XinshouCombatLocalMechanics.PanelVisibility.VISIBLE, result.observedPanel());
        assertEquals(
                List.of("CONTAINS:1246,777", "ALT_B", "WAIT:1000", "PROBE"),
                port.events);
        assertFalse(port.events.stream().anyMatch(value -> value.startsWith("CLICK:")));
    }

    @Test
    void captureAltBFailureStopsBeforeWaitProbeAndClick() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.altBSuccess = false;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(XinshouCombatLocalMechanics.Status.INPUT_FAILED, result.status());
        assertEquals(List.of("CONTAINS:1246,777", "ALT_B"), port.events);
    }

    @Test
    void captureUnavailableIsDistinctFromPanelAbsent() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.UNAVAILABLE;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(
                XinshouCombatLocalMechanics.Status.CAPTURE_UNAVAILABLE,
                result.status());
        assertEquals(
                List.of("CONTAINS:1246,777", "ALT_B", "WAIT:1000", "PROBE"),
                port.events);
    }

    @Test
    void restoreUsesTwoAltAInputsAndRequiresVisiblePanel() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.VISIBLE;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.restoreAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(XinshouCombatLocalMechanics.PanelVisibility.VISIBLE, result.observedPanel());
        assertEquals(
                List.of("ALT_A", "WAIT:1000", "ALT_A", "WAIT:1000", "PROBE"),
                port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
    }

    @Test
    void restoreAbsentIsTerminalAndDoesNotRetry() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.ABSENT;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.restoreAutoCombatOnce();

        assertEquals(
                XinshouCombatLocalMechanics.Status.PANEL_NOT_VISIBLE,
                result.status());
        assertEquals(
                List.of("ALT_A", "WAIT:1000", "ALT_A", "WAIT:1000", "PROBE"),
                port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void restoreSecondAltAFailureStopsBeforeFinalProbe() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.failAltAAtCall = 2;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.restoreAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.INPUT_FAILED, result.status());
        assertEquals(List.of("ALT_A", "WAIT:1000", "ALT_A"), port.events);
    }

    @Test
    void captureRejectsInvalidPointBeforeAnyInput() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.containsPoint = false;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(
                XinshouCombatLocalMechanics.Status.INVALID_CLICK_POINT,
                result.status());
        assertEquals(List.of("CONTAINS:1246,777"), port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
    }

    @Test
    void captureClickFailureIsTerminalAndDoesNotRetry() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.ABSENT;
        port.clickSuccess = false;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(1246, 777, 0, 0, 1920, 1080);

        assertEquals(XinshouCombatLocalMechanics.Status.INPUT_FAILED, result.status());
        assertEquals(
                List.of("CONTAINS:1246,777", "ALT_B", "WAIT:1000", "PROBE", "CLICK:1246,777"),
                port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void captureTranslatesSourcePointForWindowOnlyMovementInsideFrozenCallback() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.windowLeft = 300;
        port.windowTop = 400;
        port.windowWidth = 1024;
        port.windowHeight = 768;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(
                        321, 654,
                        100, 200, 1024, 768);

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(
                List.of("CONTAINS:521,854", "ALT_B", "WAIT:1000", "PROBE", "CLICK:521,854"),
                port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void captureRejectsWindowSizeChangeBeforeAnyInput() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.windowLeft = 300;
        port.windowTop = 400;
        port.windowWidth = 1000;
        port.windowHeight = 768;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.captureCombatOnce(
                        321, 654,
                        100, 200, 1024, 768);

        assertEquals(
                XinshouCombatLocalMechanics.Status.WINDOW_SIZE_CHANGED,
                result.status());
        assertEquals(List.of(), port.events);
        assertEquals(List.of("FOCUSED"), port.executionModes);
        assertEquals(1, port.executionCount);
    }

    @Test
    void runnerMaintenanceDoesNotPressAlt8WhenPanelIsAlreadyVisible() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelVisibility =
                XinshouCombatLocalMechanics.PanelVisibility.VISIBLE;
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.maintainRunnerAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(List.of("PROBE"), port.events);
        assertEquals(List.of("BACKGROUND"), port.executionModes);
    }

    @Test
    void runnerMaintenancePressesOneAlt8AndRequiresVisibleRecheck() {
        FakeExactWindowPort port = new FakeExactWindowPort();
        port.panelResults.add(XinshouCombatLocalMechanics.PanelVisibility.ABSENT);
        port.panelResults.add(XinshouCombatLocalMechanics.PanelVisibility.VISIBLE);
        XinshouCombatLocalMechanics mechanics = new XinshouCombatLocalMechanics(port);

        XinshouCombatLocalMechanics.Result result =
                mechanics.maintainRunnerAutoCombatOnce();

        assertEquals(XinshouCombatLocalMechanics.Status.COMPLETED, result.status());
        assertEquals(
                List.of("PROBE", "ALT_8", "WAIT:500", "PROBE"),
                port.events);
        assertEquals(List.of("BACKGROUND"), port.executionModes);
    }

    private static final class FakeExactWindowPort
            implements XinshouCombatLocalMechanics.ExactWindowPort,
            XinshouCombatLocalMechanics.ExactWindowSession {

        private final List<String> events = new ArrayList<>();
        private final List<String> executionModes = new ArrayList<>();
        private XinshouCombatLocalMechanics.PanelVisibility panelVisibility =
                XinshouCombatLocalMechanics.PanelVisibility.ABSENT;
        private XinshouCombatLocalMechanics.CombatVisibility combatVisibility =
                XinshouCombatLocalMechanics.CombatVisibility.VISIBLE;
        private final java.util.Deque<XinshouCombatLocalMechanics.PanelVisibility>
                panelResults = new java.util.ArrayDeque<>();
        private boolean altBSuccess = true;
        private boolean alt8Success = true;
        private boolean clickSuccess = true;
        private boolean containsPoint = true;
        private int windowLeft;
        private int windowTop;
        private int windowWidth = 1920;
        private int windowHeight = 1080;
        private int failAltAAtCall = -1;
        private int altACalls;
        private int executionCount;

        @Override
        public XinshouCombatLocalMechanics.Result executeBackground(
                String description,
                XinshouCombatLocalMechanics.ExactWindowAction action) {
            executionModes.add("BACKGROUND");
            executionCount++;
            return action.execute(this);
        }

        @Override
        public XinshouCombatLocalMechanics.Result executeFocused(
                String description,
                XinshouCombatLocalMechanics.ExactWindowAction action) {
            executionModes.add("FOCUSED");
            executionCount++;
            return action.execute(this);
        }

        @Override
        public XinshouCombatLocalMechanics.CombatVisibility probeCombatVisible() {
            return combatVisibility;
        }

        @Override
        public boolean pressAltA() {
            events.add("ALT_A");
            altACalls++;
            return altACalls != failAltAAtCall;
        }

        @Override
        public boolean pressAltB() {
            events.add("ALT_B");
            return altBSuccess;
        }

        @Override
        public boolean pressAlt8() {
            events.add("ALT_8");
            return alt8Success;
        }

        @Override
        public boolean waitMillis(int millis) {
            events.add("WAIT:" + millis);
            return true;
        }

        @Override
        public XinshouCombatLocalMechanics.PanelVisibility probeAutoRemaining() {
            events.add("PROBE");
            return panelResults.isEmpty() ? panelVisibility : panelResults.removeFirst();
        }

        @Override
        public int windowLeft() {
            return windowLeft;
        }

        @Override
        public int windowTop() {
            return windowTop;
        }

        @Override
        public int windowWidth() {
            return windowWidth;
        }

        @Override
        public int windowHeight() {
            return windowHeight;
        }

        @Override
        public boolean containsScreenPoint(int screenX, int screenY) {
            events.add("CONTAINS:" + screenX + "," + screenY);
            return containsPoint;
        }

        @Override
        public boolean clickAbsolute(int screenX, int screenY) {
            events.add("CLICK:" + screenX + "," + screenY);
            return clickSuccess;
        }
    }
}
