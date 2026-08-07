package com.bot.dhxy.cloud.turn.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnXinshouTrackerChainProtocolContractTest {

    @Test
    void validatorAcceptsOneAbsolutePointInsideACompleteSourceWindow() {
        assertValid(new TurnXinshouTrackerChainArguments(
                "xinshou:tracker",
                List.of(new TurnXinshouTrackerLink(-1000, 300)),
                -1200, 100, 1024, 768));
    }

    @Test
    void compatibilityConstructorAndInvalidGeometryFailClosed() {
        assertRejected(new TurnXinshouTrackerChainArguments(
                "xinshou:legacy",
                List.of(new TurnXinshouTrackerLink(100, 100))));
        assertRejected(new TurnXinshouTrackerChainArguments(
                "xinshou:zero-width",
                List.of(new TurnXinshouTrackerLink(100, 100)),
                0, 0, 0, 768));
        assertRejected(new TurnXinshouTrackerChainArguments(
                "xinshou:outside",
                List.of(new TurnXinshouTrackerLink(1024, 100)),
                0, 0, 1024, 768));
        assertRejected(new TurnXinshouTrackerChainArguments(
                "xinshou:no-link",
                List.of(),
                0, 0, 1024, 768));
        assertRejected(new TurnXinshouTrackerChainArguments(
                "xinshou:two-links",
                List.of(
                        new TurnXinshouTrackerLink(100, 100),
                        new TurnXinshouTrackerLink(200, 200)),
                0, 0, 1024, 768));
    }

    private static void assertValid(TurnXinshouTrackerChainArguments arguments) {
        assertDoesNotThrow(() -> TurnProtocolValidator.requireValid(action(arguments)));
    }

    private static void assertRejected(TurnXinshouTrackerChainArguments arguments) {
        assertThrows(IllegalArgumentException.class,
                () -> TurnProtocolValidator.requireValid(action(arguments)));
    }

    private static TurnAction action(TurnXinshouTrackerChainArguments arguments) {
        return TurnProtocolGoldenSupport.action(
                "xinshou-tracker-chain",
                List.of(TurnProtocolGoldenSupport.localStep(
                        0,
                        new TurnLocalServiceCall(
                                TurnLocalOperation.XINSHOU_TRACKER_LINK_CHAIN,
                                arguments))));
    }
}
