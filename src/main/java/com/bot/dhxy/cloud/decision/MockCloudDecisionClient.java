package com.bot.dhxy.cloud.decision;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockCloudDecisionClient implements CloudDecisionClient {

    private static final String DEFAULT_POLICY_VERSION = "mock-local";
    private static final String DEFAULT_DECISION = "LOCAL";

    @Override
    public CloudDecisionResponse decide(CloudDecisionRequest request) {
        String policyVersion = hasText(request.getPolicyVersion())
                ? request.getPolicyVersion()
                : DEFAULT_POLICY_VERSION;
        String decision = hasText(request.getLocalDecision())
                ? request.getLocalDecision()
                : DEFAULT_DECISION;

        return CloudDecisionResponse.builder()
                .serviceId(request.getServiceId())
                .traceId(request.getTraceId())
                .policyVersion(policyVersion)
                .decision(decision)
                .confidence(1.0d)
                .ttlMs(1_000L)
                .diagnostics(Map.of("client", "mock"))
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
