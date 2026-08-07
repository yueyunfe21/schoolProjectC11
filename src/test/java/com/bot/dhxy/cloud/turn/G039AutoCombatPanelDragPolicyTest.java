package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputCoordinateSpace;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class G039AutoCombatPanelDragPolicyTest {

    @Test
    void onlyTypedPanelDragMayBypassCombatMouseFence() {
        TurnInputSpec markedPanelDrag = markedPanelDrag();

        assertTrue(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.DRAG_LEFT, markedPanelDrag));
        assertFalse(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.DRAG_LEFT,
                new TurnInputSpec(140, 240, 180, 260, null, null, null)));
        assertFalse(TurnInputStepExecutor.allowsCombatActiveMouse(
                TurnInputAction.CLICK_LEFT, markedPanelDrag));
    }

    @Test
    void protocolRejectsPanelMarkerOnNonDragInput() throws Exception {
        Method requireInput = TurnProtocolValidator.class.getDeclaredMethod(
                "requireInput", TurnInputAction.class, TurnInputSpec.class);
        requireInput.setAccessible(true);

        assertDoesNotThrow(() -> requireInput.invoke(
                null, TurnInputAction.DRAG_LEFT, markedPanelDrag()));
        InvocationTargetException rejected = assertThrows(
                InvocationTargetException.class,
                () -> requireInput.invoke(null, TurnInputAction.CLICK_LEFT, markedPanelDrag()));
        assertInstanceOf(IllegalArgumentException.class, rejected.getCause());
    }

    private static TurnInputSpec markedPanelDrag() {
        return new TurnInputSpec(
                140,
                240,
                180,
                260,
                null,
                null,
                null,
                null,
                null,
                null,
                TurnInputCoordinateSpace.WINDOW_RELATIVE,
                true);
    }
}
