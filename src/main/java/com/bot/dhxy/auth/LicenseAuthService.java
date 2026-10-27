package com.bot.dhxy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bot.dhxy.core.http.BoundedHttpBodyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseAuthService {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BEIJING_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss '北京时间'").withZone(BEIJING_ZONE);

    private final DeviceFingerprintService deviceFingerprintService;
    private final RuntimeAuthorizationState runtimeAuthorization;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final AtomicReference<String> latestVerifiedLicenseCode = new AtomicReference<>("");

    @Value("${cloud.turn.device-id:dhxy-client}")
    private String runtimeClientId;
    @Value("${cloud.turn.bearer-token:}")
    private String runtimeClientToken;

    @Value("${license.worker.base-url:https://dhxy-license-worker.yueyunfe.workers.dev}")
    private String baseUrl;

    @Value("${license.worker.app-id:dhxy}")
    private String appId;

    @Value("${license.worker.app-version:0.0.1}")
    private String appVersion;

    /**
     * Verify on explicit UI action, then open a dedicated runtime session in formal mode.
     * @param licenseCode user-entered license code; null/blank is rejected without network access.
     * @return UI result; a formal success requires matching-device verification AND session OPEN.
     */
    public synchronized LicenseAuthResult verify(String licenseCode) {
        long loginGeneration;
        try { loginGeneration = runtimeAuthorization.requiresSession() ? runtimeAuthorization.beginLogin() : -1L; }
        catch (IllegalStateException cleanupPending) {
            return LicenseAuthResult.failure("CLIENT_SESSION_INVALID", cleanupPending.getMessage());
        }
        if (licenseCode == null || licenseCode.isBlank()) {
            return LicenseAuthResult.failure("LICENSE_CODE_REQUIRED", "请输入授权码。");
        }

        ObjectNode payload = basePayload(licenseCode);
        LicenseAuthResult verified = post("/api/license/verify", payload);
        if (!verified.success() || !runtimeAuthorization.requiresSession()) return verified;
        if (!Boolean.TRUE.equals(verified.currentDeviceMatched())) {
            return LicenseAuthResult.failure("DEVICE_MISMATCH", "授权电脑未明确匹配，不能建立运行会话。");
        }
        try {
            UUID id = UUID.fromString(runtimeClientId);
            if (!id.toString().equals(runtimeClientId) || !runtimeClientToken.matches("[A-Za-z0-9_-]{43,128}")) {
                return LicenseAuthResult.failure("CLIENT_BINDING_REQUIRED", "请先配置维护者分配的专属大脑连接。");
            }
            ObjectNode session = objectMapper.createObjectNode();
            session.put("operation", "OPEN");
            session.put("clientId", runtimeClientId);
            session.put("clientToken", runtimeClientToken);
            session.put("licenseCode", licenseCode.trim());
            session.put("deviceFingerprint", deviceFingerprintService.getDeviceFingerprint());
            session.put("appVersion", appVersion);
            // The runtime trust authority is pinned, not taken from an arbitrary connection URL.
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "https://dhxy-license-worker.yueyunfe.workers.dev/api/client/session"))
                    .timeout(Duration.ofSeconds(12)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(session))).build();
            CompletableFuture<HttpResponse<byte[]>> pending = httpClient.sendAsync(request, new BoundedHttpBodyHandler(256 * 1024));
            HttpResponse<byte[]> response;
            try { response = pending.get(12, TimeUnit.SECONDS); }
            finally { if (!pending.isDone()) pending.cancel(true); }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (response.statusCode() != 200 || !root.path("ok").asBoolean(false)) {
                return LicenseAuthResult.failure(root.path("code").asText("CLIENT_SESSION_DENIED"),
                        root.path("message").asText("无法获得运行许可，请重新验证授权或更新客户端。"));
            }
            String token = data.path("sessionToken").asText("");
            if (!"CLIENT_SESSION_ALLOWED".equals(root.path("code").asText()) ||
                    !runtimeClientId.equals(data.path("clientId").asText()) ||
                    !"dhxy".equals(data.path("appId").asText()) || !appVersion.equals(data.path("appVersion").asText()) ||
                    !token.matches("[A-Za-z0-9_-]{43,128}")) {
                return LicenseAuthResult.failure("CLIENT_SESSION_INVALID", "授权服务返回的运行会话身份不匹配。");
            }
            if (!runtimeAuthorization.activate(loginGeneration, token)) {
                return LicenseAuthResult.failure("CLIENT_SESSION_INVALID", "登录结果已失效，请重新验证授权。");
            }
            return verified;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return LicenseAuthResult.failure("INTERRUPTED", "运行授权请求已中断。");
        } catch (Exception unavailable) {
            // No request/response or exception details: they may contain a bearer or license value.
            return LicenseAuthResult.failure("AUTHORIZATION_UNAVAILABLE", "运行授权暂时不可用；未获得任务许可。");
        }
    }

    public LicenseAuthResult refreshStatus(String licenseCode) {
        if (licenseCode == null || licenseCode.isBlank()) {
            return LicenseAuthResult.failure("LICENSE_CODE_REQUIRED", "请输入授权码。");
        }

        return post("/api/license/status", basePayload(licenseCode));
    }

    public LicenseAuthResult renew30Days(String licenseCode) {
        if (licenseCode == null || licenseCode.isBlank()) {
            return LicenseAuthResult.failure("LICENSE_CODE_REQUIRED", "请输入授权码。");
        }

        ObjectNode payload = basePayload(licenseCode);
        payload.put("days", 30);
        return post("/api/license/renew", payload);
    }

    public String getAppId() {
        return appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getLatestVerifiedLicenseCode() {
        return latestVerifiedLicenseCode.get();
    }

    private ObjectNode basePayload(String licenseCode) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appId", appId);
        // Version admission must see the same build on verify AND later status refreshes.
        payload.put("appVersion", appVersion);
        payload.put("licenseCode", licenseCode.trim());
        payload.put("deviceFingerprint", deviceFingerprintService.getDeviceFingerprint());
        return payload;
    }

    private LicenseAuthResult post(String path, ObjectNode payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            boolean ok = root.path("ok").asBoolean(false);
            String code = root.path("code").asText("");
            String message = root.path("message").asText(ok ? "授权操作成功。" : "授权操作失败。");
            String actionType = root.path("actionType").asText("");
            JsonNode data = root.path("data");
            String expiresAt = data.path("expiresAt").asText("");
            String responseLicenseCode = data.path("licenseCode").asText(payload.path("licenseCode").asText(""));
            if (ok && responseLicenseCode != null && !responseLicenseCode.isBlank()) {
                latestVerifiedLicenseCode.set(responseLicenseCode.trim());
            }

            return new LicenseAuthResult(
                    ok,
                    code,
                    mapMessage(code, message),
                    LicenseActionType.fromCode(actionType.isBlank() ? code : actionType),
                    data.path("appId").asText(appId),
                    responseLicenseCode,
                    formatExpireText(expiresAt),
                    normalize(expiresAt),
                    data.path("bound").asBoolean(false),
                    data.hasNonNull("currentDeviceMatched") ? data.path("currentDeviceMatched").asBoolean() : null
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LicenseAuthResult.failure("INTERRUPTED", "授权请求被中断。");
        } catch (Exception e) {
            log.warn("DHXY license worker request failed: path={}", path, e);
            return LicenseAuthResult.failure("CLIENT_ERROR", "授权服务请求失败：" + e.getMessage());
        }
    }

    private String mapMessage(String code, String serverMessage) {
        if ("LICENSE_APP_MISMATCH".equals(code)) {
            return "该授权码不属于 DHXY 主项目，请使用 DHXY 授权码。";
        }
        return serverMessage == null || serverMessage.isBlank() ? "授权操作失败。" : serverMessage;
    }

    private String formatExpireText(String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank() || "null".equalsIgnoreCase(expiresAt)) {
            return "永久有效";
        }

        try {
            return BEIJING_TIME_FORMATTER.format(Instant.parse(expiresAt));
        } catch (Exception e) {
            return expiresAt;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
