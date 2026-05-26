package com.bot.dhxy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseAuthService {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BEIJING_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss '北京时间'").withZone(BEIJING_ZONE);

    private final DeviceFingerprintService deviceFingerprintService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${license.worker.base-url:https://dhxy-license-worker.yueyunfe.workers.dev}")
    private String baseUrl;

    @Value("${license.worker.app-id:dhxy}")
    private String appId;

    @Value("${license.worker.app-version:0.0.1}")
    private String appVersion;

    public LicenseAuthResult verify(String licenseCode) {
        if (licenseCode == null || licenseCode.isBlank()) {
            return LicenseAuthResult.failure("LICENSE_CODE_REQUIRED", "请输入授权码。");
        }

        ObjectNode payload = basePayload(licenseCode);
        payload.put("appVersion", appVersion);
        return post("/api/license/verify", payload);
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

    private ObjectNode basePayload(String licenseCode) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appId", appId);
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

            return new LicenseAuthResult(
                    ok,
                    code,
                    mapMessage(code, message),
                    LicenseActionType.fromCode(actionType.isBlank() ? code : actionType),
                    data.path("appId").asText(appId),
                    data.path("licenseCode").asText(payload.path("licenseCode").asText("")),
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
