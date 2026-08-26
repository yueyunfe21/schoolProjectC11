package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BagLocalOperationExecutorContractTest {

    @Test
    void fiveRingSupplyUsesOneGuardedOpenSessionAndCloudContinuation() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java"));
        int open = source.indexOf("bagService.withMainBagOpenGuarded(");
        int incense = source.indexOf("continueIncenseInsideOpenBag(", open);
        int checkpoint = source.indexOf("stopToken.throwIfStopRequested()", incense);
        int count = source.indexOf("mainBag.countItemUpTo(", checkpoint);
        assertTrue(open >= 0 && open < incense && incense < checkpoint && checkpoint < count);
        assertTrue(source.contains("TurnContinuationRequest.Stage.STATUS_IMAGE"));
        assertTrue(source.contains("TurnContinuationRequest.Stage.OUTCOME_USED"));
        assertFalse(source.contains("PlayerStateService"));
        assertFalse(source.contains("submitAndWait("));
    }

    @Test
    void returnItemChecksExactRunnerCombatStateBeforeCallingBagMacro() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java"));
        int method = source.indexOf("private LocalServiceExecution executeReturnItem(");
        int combatGuard = source.indexOf("context.isLocalCombatVisible()", method);
        int rejection = source.indexOf("\"LOCAL_COMBAT_ACTIVE\"", combatGuard);
        int macro = source.indexOf("bagService.runReturnItemMacroDirectForExclusive(", method);

        assertTrue(method >= 0 && method < combatGuard && combatGuard < rejection && rejection < macro);
    }
}
