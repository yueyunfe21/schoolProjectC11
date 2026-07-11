package com.bot.dhxy.cloud.decision;

/**
 * Service-specific execute gate for a schema-valid cloud response.
 *
 * <p>The coordinator owns the common cloud availability, timeout, schema, and percent gates. A
 * service that needs a stricter business boundary supplies this gate so execute mode can only
 * produce an effective decision after mapping the cloud response back to local, already-safe data.</p>
 */
public interface CloudDecisionExecutionGate {

    /**
     * Returns whether this gate may execute decisions for the service.
     *
     * @param serviceId cloud service id from the current request; may be {@code null}
     * @return true only for service ids that this gate is allowed to execute.
     */
    boolean allowsExecution(CloudDecisionServiceId serviceId);

    /**
     * Maps a schema-valid cloud response to a locally safe effective decision.
     *
     * @param request original cloud request; contains trace/task context and the local decision
     * @param response schema-valid cloud response with matching service id and trace id
     * @param localDecision local decision that remains the fallback for any rejection
     * @return accepted result with a locally safe effective decision, or rejected result with a reason
     */
    GateResult evaluate(CloudDecisionRequest request, CloudDecisionResponse response, String localDecision);

    record GateResult(boolean accepted, String effectiveDecision, String reason) {
        public static GateResult accepted(String effectiveDecision, String reason) {
            return new GateResult(true, effectiveDecision, reason);
        }

        public static GateResult rejected(String reason) {
            return new GateResult(false, null, reason);
        }
    }
}
