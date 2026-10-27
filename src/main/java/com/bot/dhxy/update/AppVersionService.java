package com.bot.dhxy.update;

import com.bot.dhxy.model.update.ClientUpdateNotice;
import com.bot.dhxy.core.http.BoundedHttpBodyHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AppVersionService {
    private static final Pattern VERSION = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-b([1-9]\\d*)\\.([1-9]\\d*))?$");
    private final String currentVersion;
    private final String policyBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public AppVersionService(@Value("${bot.app-version:0.1.0-dev}") String currentVersion,
            @Value("${license.worker.base-url:https://dhxy-license-worker.yueyunfe.workers.dev}") String policyBaseUrl) {
        this.currentVersion = currentVersion;
        this.policyBaseUrl = policyBaseUrl;
    }

    /**
     * Returns the application version displayed in the JavaFX shell.
     *
     * @return local build version stamped by the release pipeline, or the development version.
     */
    public String currentVersion() {
        return currentVersion;
    }

    /**
     * Read published-version metadata over HTTPS. Must be called off the JavaFX/input threads;
     * no license/device/token is uploaded, and no application files are downloaded or executed.
     * @return non-null informational result; network/configuration failure never means "up to date".
     */
    public ClientUpdateNotice checkForUpdate() {
        try {
            URI base = URI.create(policyBaseUrl);
            if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null ||
                    base.getQuery() != null || base.getFragment() != null) throw new IllegalArgumentException("Invalid policy HTTPS endpoint");
            URI endpoint = base.resolve("/api/client/version-policy?appId=dhxy");
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json").GET().build();
            var pending = httpClient.sendAsync(request, new BoundedHttpBodyHandler(262144));
            HttpResponse<byte[]> response;
            try { response = pending.get(8, java.util.concurrent.TimeUnit.SECONDS); }
            finally { if (!pending.isDone()) pending.cancel(true); }
            if (response.statusCode() != 200) throw new IllegalStateException("Version service unavailable");
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!root.path("ok").asBoolean() || !"dhxy".equals(data.path("appId").asText()) ||
                    !data.path("latestVersion").isTextual() || !data.path("releaseUrl").isTextual()) {
                throw new IllegalStateException("Invalid version policy response");
            }
            String latest = data.path("latestVersion").asText();
            String link = data.path("releaseUrl").asText();
            boolean updateRequired = !permitsVersion(data, currentVersion);
            if (latest.isEmpty() && link.isEmpty()) {
                return ClientUpdateNotice.builder().status(updateRequired ? ClientUpdateNotice.Status.UPDATE_REQUIRED : ClientUpdateNotice.Status.NO_RELEASE)
                        .message(updateRequired ? "此版本已不被允许使用，需要更新；维护者尚未公布可下载的正式版本。" : "目前尚未公布正式客户端版本。").build();
            }
            BigInteger[] targetParts = versionParts(latest);
            // Only the owner-approved repository's exact version page may be opened, never a
            // command, arbitrary URL, redirect supplied in a policy, or unsigned install script.
            String expectedLink = "https://github.com/yueyunfe21/schoolProjectC11/releases/tag/client-" + latest;
            if (targetParts == null || !expectedLink.equals(link)) throw new IllegalStateException("Untrusted release page");
            if (!permitsVersion(data, latest)) throw new IllegalStateException("Recommended release is not allowed");
            URI page = URI.create(link);
            BigInteger[] installedParts = versionParts(currentVersion);
            if (installedParts == null) {
                return ClientUpdateNotice.builder().status(ClientUpdateNotice.Status.DEVELOPMENT_BUILD)
                        .latestVersion(latest).releasePage(page)
                        .message("当前为开发版本 " + currentVersion + "；已公布正式版 " + latest + "。开发版不自动替换。").build();
            }
            if (updateRequired) {
                return ClientUpdateNotice.builder().status(ClientUpdateNotice.Status.UPDATE_REQUIRED)
                        .latestVersion(latest).releasePage(page)
                        .message("此版本已不被允许使用，请更新到 " + latest + "。暂不更新可保留界面，但不能运行任务。").build();
            }
            boolean newer = false;
            for (int i = 0; i < targetParts.length; i++) {
                int comparison = targetParts[i].compareTo(installedParts[i]);
                if (comparison != 0) { newer = comparison > 0; break; }
            }
            return ClientUpdateNotice.builder().status(newer ? ClientUpdateNotice.Status.NEW_RELEASE : ClientUpdateNotice.Status.NO_NEWER_RELEASE)
                    .latestVersion(latest).releasePage(newer ? page : null)
                    .message(newer ? "发现新版本 " + latest + "。可选择下载更新或稍后；下载完成后还需确认退出安装。"
                            : "当前版本 " + currentVersion + "，未发现更新的正式版本。")
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ClientUpdateNotice.builder().status(ClientUpdateNotice.Status.UNAVAILABLE).message("更新检查已中断。").build();
        } catch (Exception exception) {
            log.warn("Client update check unavailable: {}", exception.getClass().getSimpleName());
            return ClientUpdateNotice.builder().status(ClientUpdateNotice.Status.UNAVAILABLE)
                    .message("暂时无法检查更新，请稍后重试；这不代表已是最新版。").build();
        }
    }

    /** Informational policy evaluation only; the Cloud/Worker runtime gate remains authoritative. */
    private static boolean permitsVersion(JsonNode policy, String version) {
        if (!policy.path("enforced").isBoolean()) throw new IllegalArgumentException("Invalid policy flag");
        if (!policy.path("enforced").asBoolean()) return true;
        BigInteger[] value = versionParts(version);
        if (value == null) return false;
        JsonNode revoked = policy.path("revokedVersions");
        JsonNode allowed = policy.path("allowedVersions");
        if (!revoked.isArray() || !allowed.isArray() || revoked.size() > 500 || allowed.size() > 500 ||
                !policy.path("minimumVersion").isTextual()) throw new IllegalArgumentException("Invalid policy lists");
        for (JsonNode item : revoked) {
            if (!item.isTextual() || versionParts(item.asText()) == null) throw new IllegalArgumentException("Invalid revoked version");
            if (version.equals(item.asText())) return false;
        }
        boolean explicitlyAllowed = allowed.isEmpty();
        for (JsonNode item : allowed) {
            if (!item.isTextual() || versionParts(item.asText()) == null) throw new IllegalArgumentException("Invalid allowed version");
            if (version.equals(item.asText())) explicitlyAllowed = true;
        }
        if (!explicitlyAllowed) return false;
        String minimum = policy.path("minimumVersion").asText();
        if (minimum.isEmpty()) return true;
        BigInteger[] floor = versionParts(minimum);
        if (floor == null) throw new IllegalArgumentException("Invalid minimum version");
        for (int i = 0; i < value.length; i++) {
            int order = value[i].compareTo(floor[i]);
            if (order != 0) return order > 0;
        }
        return true;
    }

    private static BigInteger[] versionParts(String version) {
        if (version == null || version.length() > 100) return null;
        var match = VERSION.matcher(version);
        if (!match.matches()) return null;
        BigInteger[] parts = new BigInteger[5];
        for (int i = 0; i < parts.length; i++) parts[i] = new BigInteger(match.group(i + 1) == null ? "0" : match.group(i + 1));
        return parts;
    }
}
