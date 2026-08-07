package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputCoordinateSpace;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.input.action.InputActionType;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnInputStepExecutorContractTest {

    private final TurnInputActionMapper mapper = new TurnInputActionMapper();
    private final TurnWindowRect window = new TurnWindowRect(100, 200, 800, 600);

    @Test
    void clickAndQueueHoldStayInOneOrderedPhysicalSequence() {
        var actions = mapper.mapMouse(TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(140, 240, null, null, null, null, null, 150, 500), window);
        assertEquals(List.of(InputActionType.CLICK_LEFT, InputActionType.SLEEP),
                actions.stream().map(a -> a.getType()).toList());
    }

    @Test
    void scrollKeepsMoveAndWheelAtomic() {
        var actions = mapper.mapMouse(TurnInputAction.SCROLL,
                new TurnInputSpec(140, 240, null, null, -3, null, null), window);
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SCROLL_UP),
                actions.stream().map(a -> a.getType()).toList());
    }

    @Test
    void keyboardAndOutOfWindowCoordinatesFailClosedInMouseMapper() {
        assertThrows(IllegalArgumentException.class, () -> mapper.mapMouse(
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "ENTER", null), window));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapMouse(
                TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(99, 240, null, null, null, null, null), window));
    }

    @Test
    void windowRelativeMiniMapPointUsesTheFreshLocalWindowRectangle() {
        TurnWindowRect freshWindow = new TurnWindowRect(1374, 134, 1036, 783);
        TurnInputSpec point = new TurnInputSpec(
                462, 576, null, null, null, null, null,
                null, null, null, TurnInputCoordinateSpace.WINDOW_RELATIVE);

        var actions = mapper.mapMouse(TurnInputAction.CLICK_LEFT, point, freshWindow);

        assertEquals(1836, actions.get(0).getX());
        assertEquals(710, actions.get(0).getY());
    }

    @Test
    void partialMouseProgressKeepsCompletedClickOutOfNotRunSuffix() {
        InputActionExecutionResult partial = InputActionExecutionResult.builder()
                .started(true)
                .startedStepIndex(0)
                .lastCompletedStepIndex(2)
                .status(InputActionExecutionResult.Status.PARTIALLY_COMPLETED)
                .build();

        assertEquals(2, TurnInputStepExecutor.completedTurnStepCount(
                List.of(0, 2, 4), partial));
    }

    @Test
    void noStartedMouseActionKeepsWholeTurnSequenceNotRunAfterTerminalStep() {
        InputActionExecutionResult notStarted = InputActionExecutionResult.builder()
                .started(false)
                .startedStepIndex(-1)
                .lastCompletedStepIndex(-1)
                .status(InputActionExecutionResult.Status.NOT_STARTED)
                .build();

        assertEquals(0, TurnInputStepExecutor.completedTurnStepCount(
                List.of(0, 2, 4), notStarted));
    }

    @Test
    void onlyTypedAutoCombatPanelDragBypassesCombatMouseFence() {
        TurnInputSpec markedPanelDrag = new TurnInputSpec(
                140, 240, 180, 260, null, null, null,
                null, null, null, null, true);

        assertTrue(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.DRAG_LEFT, markedPanelDrag));
        assertFalse(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.DRAG_LEFT,
                new TurnInputSpec(140, 240, 180, 260, null, null, null)));
        assertFalse(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.CLICK_LEFT, markedPanelDrag));
    }
}
