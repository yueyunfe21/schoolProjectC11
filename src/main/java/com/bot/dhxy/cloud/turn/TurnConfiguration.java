package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Inert Spring wiring for the explicit HTTPS turn path. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({TurnClientProperties.class, CloudTurnSidecarProperties.class})
public class TurnConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

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
    TurnLoopFactory turnLoopFactory(TurnClient turnClient,
                                    LocalTurnActionExecutor actionExecutor,
                                    AutomationMetricsService metricsService) {
        return new TurnLoopFactory(turnClient, actionExecutor, null, (windowId, event) -> metricsService.record(
                AutomationMetricEvent.builder()
                        .runId(event.startRequestId())
                        .taskCode(event.taskCode())
                        .taskName(event.taskName())
                        .windowId(windowId)
                        .phase(event.phase())
                        .eventType(AutomationMetricEventType.TASK_QUEUE_EVENT)
                        .status(toMetricStatus(event.result()))
                        .elapsedMs(event.elapsedMs())
                        .errorCode(event.exceptionType())
                        .message(event.reason())
                        .attributes(java.util.Map.of(
                                "eventId", event.eventId(),
                                "taskRunId", event.taskRunId(),
                                "queueIndex", Integer.toString(event.queueIndex()),
                                "type", event.type().name()))
                        .build()));
    }

    private static AutomationMetricStatus toMetricStatus(String result) {
        if (result == null || result.isBlank()) {
            return AutomationMetricStatus.INFO;
        }
        try {
            return AutomationMetricStatus.valueOf(result);
        } catch (IllegalArgumentException ignored) {
            return AutomationMetricStatus.INFO;
        }
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
        return new TurnModeGuard(
                taskManager, loopRegistry, properties.getLongWaitTimeoutMs(), properties.getDeviceId());
    }
}
