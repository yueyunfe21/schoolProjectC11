package com.bot.dhxy.cloud.turn;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** Local development launcher settings for the external Cloud Brain process. */
@ConfigurationProperties(prefix = "cloud.turn.sidecar")
public class CloudTurnSidecarProperties {

    private boolean autoStartEnabled = true;
    private Path scriptPath = Path.of("scripts", "run-cloud-brain-server.ps1");
    /**
     * Unset means "derive from the client project root's sibling {@code dhxy-cloud-brain}".
     * A hardcoded machine path here would silently resurface whenever the property is absent.
     */
    private Path brainProjectPath;
    private Path logPath = Path.of("logs", "cloud-decision-dev-sidecar.log");
    private String tenantId = "dhxy-local";
    private String userId;
    private Path stateRoot;
    private int ocrPort = 18_762;
    private long startupTimeoutMs = 120_000L;

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    public Path getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(Path scriptPath) {
        this.scriptPath = scriptPath;
    }

    public Path getBrainProjectPath() {
        return brainProjectPath;
    }

    public void setBrainProjectPath(Path brainProjectPath) {
        this.brainProjectPath = brainProjectPath;
    }

    public Path getLogPath() {
        return logPath;
    }

    public void setLogPath(Path logPath) {
        this.logPath = logPath;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Path getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(Path stateRoot) {
        this.stateRoot = stateRoot;
    }

    public long getStartupTimeoutMs() {
        return startupTimeoutMs;
    }

    public void setStartupTimeoutMs(long startupTimeoutMs) {
        this.startupTimeoutMs = startupTimeoutMs;
    }

    public int getOcrPort() {
        return ocrPort;
    }

    public void setOcrPort(int ocrPort) {
        this.ocrPort = ocrPort;
    }
}
