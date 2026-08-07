package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WuhuanRouteCloseImmediateReturnContractTest {

    @Test
    void typedUiFlagSkipsOnlyTheRequestedPostClickSettle() throws Exception {
        String protocol = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnUiOperationArguments.java"));
        String executor = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutor.java"));
        String cleaner = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/UICleanerService.java"));

        assertTrue(protocol.contains("boolean returnImmediatelyAfterClick"));
        assertTrue(executor.contains("call.ui().returnImmediatelyAfterClick()"));
        assertTrue(cleaner.contains("boolean settleAfterClick"));
        assertTrue(cleaner.contains("if (!settleAfterClick)"));
        assertTrue(cleaner.contains("return true;"));
        assertTrue(cleaner.contains("return TaskSleep.sleep(250)"));
    }
}
