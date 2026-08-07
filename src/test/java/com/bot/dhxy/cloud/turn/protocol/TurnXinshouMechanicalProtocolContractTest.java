package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnXinshouMechanicalProtocolContractTest {

    private static final List<TurnXinshouMechanicalAction> NO_POINT_ACTIONS = List.of(
            TurnXinshouMechanicalAction.USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
            TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW,
            TurnXinshouMechanicalAction.HAND_IN_MATERIALS,
            TurnXinshouMechanicalAction.REPAIR_ITEMS_ONCE,
            TurnXinshouMechanicalAction.CLOSE_REPAIR_WINDOW,
            TurnXinshouMechanicalAction.USE_LUNHUI_ITEM_AND_START,
            TurnXinshouMechanicalAction.PRESS_ESCAPE,
            TurnXinshouMechanicalAction.PRESS_ORDINARY_AUTO_COMBAT,
            TurnXinshouMechanicalAction.RESTORE_AUTO_COMBAT);

    @Test
    void goldenJsonRoundTripPreservesClosedMechanicalShape() throws Exception {
        TurnXinshouMechanicalArguments arguments = new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CONFIRM_ADOPTION, null);
        JsonNode actual = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(arguments));
        JsonNode expected = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree("""
                {
                  "action": "CONFIRM_ADOPTION",
                  "recoveryTemplateName": null,
                  "screenX": null,
                  "screenY": null,
                  "sourceWindowLeft": null,
                  "sourceWindowTop": null,
                  "sourceWindowWidth": null,
                  "sourceWindowHeight": null
                }
                """);
        assertEquals(expected, actual);

        TurnAction action = mechanicalAction("xinshou-mechanical-golden", arguments);
        TurnAction decoded = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(action),
                TurnAction.class);
        assertEquals(action, decoded);
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(decoded));
    }

    @Test
    void validatorAcceptsEveryActionOnlyWithItsOwnArguments() {
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CONFIRM_ADOPTION, null));
        for (TurnXinshouMechanicalAction action : NO_POINT_ACTIONS) {
            assertValid(new TurnXinshouMechanicalArguments(action, null));
        }
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE,
                "quedingguan_.png"));
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE,
                "confirm.png"));
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE,
                "tiaoguo.png"));
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                null,
                -1000, 300,
                -1200, 100, 1024, 768));
        assertValid(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                null,
                -1000, 300,
                -1200, 100, 1024, 768));
    }

    @Test
    void validatorRejectsMissingMixedAndUnsupportedArguments() {
        assertRejected(null);
        assertRejected(new TurnXinshouMechanicalArguments(null, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CONFIRM_ADOPTION, "confirm.png"));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.PRESS_ESCAPE, "confirm.png"));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW, "confirm.png"));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, " "));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, "fake.png"));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                "confirm.png",
                100, 100, 0, 0, 1024, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                null,
                100, 100, 0, 0, 0, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                null,
                1024, 100, 0, 0, 1024, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                null,
                100, 100, 0, 0, 1024, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.PRESS_ESCAPE,
                null,
                100, 100, 0, 0, 1024, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.PRESS_ORDINARY_AUTO_COMBAT,
                null,
                100, 100, 0, 0, 1024, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.RESTORE_AUTO_COMBAT,
                "confirm.png"));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                "confirm.png",
                100, 100, 0, 0, 1024, 768));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                null,
                100, 100, 0, 0, 1024, null));
        assertRejected(new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                null,
                1024, 100, 0, 0, 1024, 768));

        TurnLocalServiceCall wrongOperation = new TurnLocalServiceCall(
                TurnLocalOperation.BAG_USE_INCENSE,
                new TurnXinshouMechanicalArguments(
                        TurnXinshouMechanicalAction.PRESS_ESCAPE, null));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                TurnProtocolGoldenSupport.action(
                        "xinshou-mechanical-wrong-operation",
                        List.of(TurnProtocolGoldenSupport.localStep(0, wrongOperation)))));
    }

    private static void assertValid(TurnXinshouMechanicalArguments arguments) {
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(
                mechanicalAction("xinshou-mechanical-valid-" + arguments.action(), arguments)));
    }

    private static void assertRejected(TurnXinshouMechanicalArguments arguments) {
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(
                mechanicalAction("xinshou-mechanical-invalid", arguments)));
    }

    private static TurnAction mechanicalAction(
            String actionId,
            TurnXinshouMechanicalArguments arguments) {
        return TurnProtocolGoldenSupport.action(
                actionId,
                List.of(TurnProtocolGoldenSupport.localStep(
                        0,
                        new TurnLocalServiceCall(
                                TurnLocalOperation.XINSHOU_MECHANICAL_ACTION,
                                arguments))));
    }
}
