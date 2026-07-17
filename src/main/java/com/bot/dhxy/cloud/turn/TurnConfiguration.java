package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Inert Spring wiring for the explicit HTTPS turn path. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TurnClientProperties.class)
public class TurnConfiguration {

    @Bean
    TurnClient turnClient(TurnClientProperties properties, ObjectMapper objectMapper) {
        properties.requireValid();
        return new HttpsTurnClient(
                properties.getBaseUri(),
                properties.getBearerToken(),
                properties.connectTimeout(),
                properties.requestTimeout(),
                objectMapper);
    }

    @Bean
    TurnTemplateCache turnTemplateCache(TurnClientProperties properties, TurnClient turnClient) {
        properties.requireValid();
        return new TurnTemplateCache(properties.getTemplateRoot(), turnClient);
    }

    @Bean
    TurnMatchStepExecutor turnMatchStepExecutor(TurnTemplateCache templateCache,
                                                TurnCaptureStepExecutor captureStepExecutor) {
        return new TurnMatchStepExecutor(templateCache, captureStepExecutor);
    }

    @Bean
    TurnLoopFactory turnLoopFactory(TurnClient turnClient, LocalTurnActionExecutor actionExecutor) {
        return new TurnLoopFactory(turnClient, actionExecutor);
    }

    @Bean
    TurnLoopRegistry turnLoopRegistry(TurnLoopFactory loopFactory) {
        return new TurnLoopRegistry(loopFactory);
    }

    @Bean
    TurnModeGuard turnModeGuard(TurnClientProperties properties,
                                MultiWindowTaskManager taskManager,
                                TurnLoopRegistry loopRegistry) {
        properties.requireValid();
        return new TurnModeGuard(taskManager, loopRegistry, properties.getLongWaitTimeoutMs());
    }
}
