package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalServiceStepDispatcherContractTest {

    @Test
    void dispatcherClosesEveryOperationAndKeepsHostOutsideInputQueue() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java"));
        Set<String> enumNames = Set.of(TurnLocalOperation.values()).stream()
                .map(Enum::name).collect(Collectors.toSet());
        for (String operation : enumNames) {
            assertTrue(source.contains(operation), operation + " must have an explicit route");
        }
        assertTrue(source.contains("case HOST_SLEEP_COMPUTER -> hostAdapter.execute"));
        assertFalse(source.contains("SystemPowerService"));
    }

    @Test
    void permanentBusinessServicePackageContainsExactlyFourFiles() throws Exception {
        try (var files = Files.list(Path.of("src/main/java/com/bot/dhxy/service"))) {
            assertEquals(Set.of("BagService.java", "UICleanerService.java",
                            "GiveItemService.java", "QuestManagerService.java"),
                    files.filter(path -> path.toString().endsWith(".java"))
                            .map(path -> path.getFileName().toString()).collect(Collectors.toSet()));
        }
    }
}
