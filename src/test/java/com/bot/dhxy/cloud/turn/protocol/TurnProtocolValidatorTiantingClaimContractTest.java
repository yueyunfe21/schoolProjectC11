package com.bot.dhxy.cloud.turn.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnProtocolValidatorTiantingClaimContractTest {

    @Test
    void tiantingExpectedCombatEnterClaimIsAccepted() {
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(actionFor("TIANTING")));
    }

    @Test
    void unrelatedTaskExpectedCombatEnterClaimIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(actionFor("SOME_OTHER_TASK")));
    }

    private static TurnAction actionFor(String taskCode) {
        TurnWholeTaskRuntimeArguments arguments = new TurnWholeTaskRuntimeArguments(
                "tianting-claim", null, null, null, null, null, null, null,
                null, null, null, null, null, taskCode, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                "claim-1", "observation-run-1", "business-run-1", "attempt-1", null);
        TurnLocalServiceCall call = new TurnLocalServiceCall(
                TurnLocalOperation.WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM,
                null, null, null, null, arguments);
        return new TurnAction(
                1, "tianting-expected-combat-claim", "device-1", "window-1",
                List.of(new TurnStep(0, TurnStepType.LOCAL_SERVICE, null, null, null, null, null, call)),
                false);
    }
}
