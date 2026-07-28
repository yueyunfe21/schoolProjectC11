package com.bot.dhxy.cloud.turn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnConfigurationWiringContractTest {
    @Test
    void configurationProvidesSharedObjectMapperBean() {
        ObjectMapper objectMapper = new TurnConfiguration().objectMapper();

        assertNotNull(objectMapper);
    }

    @Test
    void configurationConstructsInertBeansWithoutStartingLoops() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnConfiguration.java"));
        assertTrue(source.contains("@Configuration"));
        assertTrue(source.contains("@Bean"));
        assertFalse(source.contains(".start()"));
        assertFalse(source.contains("new Thread("));
    }
}
