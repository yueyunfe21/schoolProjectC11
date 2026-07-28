package com.bot.dhxy.cloud.turn;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/** Independent HTTPS turn transport and long-wait configuration. */
@ConfigurationProperties(prefix = "cloud.turn")
public class TurnClientProperties {

    private URI baseUri = URI.create("http://127.0.0.1:18080");
    private String bearerToken = "local-dev-token";
    private String deviceId = "dhxy-client";
    private long connectTimeoutMs = 3_000L;
    private long requestTimeoutMs = 65_000L;
    private long longWaitTimeoutMs = 60_000L;
    private Path templateRoot = Path.of("images", "template");

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public long getLongWaitTimeoutMs() {
        return longWaitTimeoutMs;
    }

    public void setLongWaitTimeoutMs(long longWaitTimeoutMs) {
        this.longWaitTimeoutMs = longWaitTimeoutMs;
    }

    public Path getTemplateRoot() {
        return templateRoot;
    }

    public void setTemplateRoot(Path templateRoot) {
        this.templateRoot = templateRoot;
    }

    Duration connectTimeout() {
        return Duration.ofMillis(connectTimeoutMs);
    }

    Duration requestTimeout() {
        return Duration.ofMillis(requestTimeoutMs);
    }

    void requireValid() {
        if (baseUri == null) {
            throw new IllegalArgumentException("cloud.turn.base-uri must be configured");
        }
        if (bearerToken == null || bearerToken.isBlank() || !bearerToken.equals(bearerToken.trim())) {
            throw new IllegalArgumentException("cloud.turn.bearer-token must be nonblank without surrounding whitespace");
        }
        if (deviceId == null || deviceId.isBlank() || !deviceId.equals(deviceId.trim())) {
            throw new IllegalArgumentException("cloud.turn.device-id must be nonblank without surrounding whitespace");
        }
        if (connectTimeoutMs <= 0L || requestTimeoutMs <= 0L || longWaitTimeoutMs <= 0L) {
            throw new IllegalArgumentException("cloud.turn timeouts must be positive");
        }
        if (requestTimeoutMs <= longWaitTimeoutMs) {
            throw new IllegalArgumentException(
                    "cloud.turn.request-timeout-ms must be greater than cloud.turn.long-wait-timeout-ms");
        }
        if (templateRoot == null) {
            throw new IllegalArgumentException("cloud.turn.template-root must be configured");
        }
    }
}
