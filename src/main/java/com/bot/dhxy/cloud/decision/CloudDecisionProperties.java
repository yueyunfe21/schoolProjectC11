package com.bot.dhxy.cloud.decision;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "cloud")
public class CloudDecisionProperties {
    private boolean enabled = false;
    private boolean realTransportEnabled = false;
    private String baseUrl = "http://127.0.0.1:18080";
    private String endpointPath = "/api/cloud/decision";
    private String routeMemoryOutcomePath = "/api/cloud/route-memory/outcome";
    private String npcClickSmartOutcomePath = "/api/cloud/npc-click-smart/outcome";
    private String token = "";
    private long timeoutMs = 60_000L;
    private CloudFallbackMode defaultFallback = CloudFallbackMode.LOCAL;
    private DevSidecar devSidecar = new DevSidecar();
    private Map<CloudDecisionServiceId, Service> services = new EnumMap<>(CloudDecisionServiceId.class);

    public Service service(CloudDecisionServiceId serviceId) {
        return services.computeIfAbsent(serviceId, ignored -> new Service());
    }

    @Data
    public static class Service {
        private boolean shadowEnabled = false;
        private boolean executeEnabled = false;
        private int executePercent = 0;
        private CloudFallbackMode fallback = CloudFallbackMode.LOCAL;
    }

    @Data
    public static class DevSidecar {
        private boolean autoStartEnabled = true;
        private boolean restartOnTaskStart = false;
        private boolean rebuildOnStart = false;
        private String brainProjectPath = "D:\\mavenProject\\dhxy-cloud-brain";
        private String scriptPath = "scripts/run-cloud-brain-server.ps1";
        private String logPath = "logs/cloud-decision-dev-sidecar.log";
        private long startupTimeoutMs = 60_000L;
    }
}
