package com.bot.dhxy.update;

import com.bot.dhxy.auth.DeviceFingerprintService;
import com.bot.dhxy.auth.UpdateCredentialStore;
import com.bot.dhxy.core.http.BoundedHttpBodyHandler;
import com.bot.dhxy.model.update.ClientUpdateNotice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit user-triggered updater; no startup download, game input, or developer-worktree changes. */
@Service
public class ClientUpdateService {
    private final String configuredRoot;
    private final String configuredApplication;
    private final String workerBaseUrl;
    private final String currentVersion;
    private final String runtimeClientId;
    private final String runtimeClientToken;
    private final DeviceFingerprintService deviceFingerprintService;
    private final UpdateCredentialStore credentialStore;
    private final AtomicBoolean downloading = new AtomicBoolean();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper json = new ObjectMapper();

    public ClientUpdateService(@Value("${dhxy.install-root:}") String root,
            @Value("${dhxy.application-root:}") String application,
            @Value("${license.worker.base-url:https://dhxy-license-worker.yueyunfe.workers.dev}") String workerBaseUrl,
            @Value("${bot.app-version:0.1.0-dev}") String currentVersion,
            @Value("${cloud.turn.device-id:dhxy-client}") String runtimeClientId,
            @Value("${cloud.turn.bearer-token:}") String runtimeClientToken,
            DeviceFingerprintService deviceFingerprintService,
            UpdateCredentialStore credentialStore) {
        configuredRoot = root;
        configuredApplication = application;
        this.workerBaseUrl = workerBaseUrl;
        this.currentVersion = currentVersion;
        this.runtimeClientId = runtimeClientId;
        this.runtimeClientToken = runtimeClientToken;
        this.deviceFingerprintService = deviceFingerprintService;
        this.credentialStore = credentialStore;
    }

    public boolean installationAvailable() {
        return !configuredRoot.isBlank() && !configuredApplication.isBlank();
    }

    /**
     * Download only after a user action, off FX/input threads; leaves failed staging for diagnostics.
     * @param notice non-null optional/required notice with an allowed recommended release; arbitrary links are not consumed.
     * @return staging directory containing the verified manifest, archive and version; not installed yet.
     */
    public Path download(ClientUpdateNotice notice) throws Exception {
        if (!downloading.compareAndSet(false, true)) throw new IOException("已有更新正在下载");
        try {
            if (notice == null || !notice.hasInstallableRelease()) throw new IOException("没有可更新版本");
            Path root = checkedRoot();
            Path updates = root.resolve(".updates");
            Files.createDirectories(updates);
            if (!updates.toRealPath().equals(updates)) throw new IOException("更新目录不能是链接");
            Path pending = Files.createDirectory(updates.resolve(UUID.randomUUID().toString()));
            String version = notice.getLatestVersion();
            if (version == null || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+-b[0-9]+\\.[0-9]+")) throw new IOException("版本格式错误");
            Path envelope = pending.resolve("update-manifest.json");
            URI archiveUri;
            if (notice.isAuthorizedDownload()) {
                UpdateTicket ticket = requestUpdateTicket(version);
                Files.write(envelope, ticket.manifestBytes(), StandardOpenOption.CREATE_NEW);
                archiveUri = ticket.downloadUri();
            } else {
                URI base = URI.create(ClientUpdatePackage.RELEASE_BASE + "client-" + version + "/");
                downloadFile(base.resolve("update-manifest.json"), envelope, 65_536);
                archiveUri = base.resolve(ClientUpdatePackage.verifyManifest(Files.readAllBytes(envelope), version)
                        .path("packageFile").asText());
            }
            JsonNode manifest = ClientUpdatePackage.verifyManifest(Files.readAllBytes(envelope), version);
            Path archive = pending.resolve(manifest.path("packageFile").asText());
            downloadFile(archiveUri, archive, manifest.path("sizeBytes").asLong());
            ClientUpdatePackage.verifyArchive(archive, manifest);
            Files.writeString(pending.resolve("version.txt"), version, StandardOpenOption.CREATE_NEW);
            return pending;
        } finally { downloading.set(false); }
    }

    /**
     * Start a separate Java process only after the user's exit/install confirmation.
     * @param pending staging directory returned by download; must belong to this installation.
     * @return process handle; caller requests orderly application shutdown only after successful spawn.
     */
    public Process startInstaller(Path pending) throws Exception {
        Path root = checkedRoot();
        Path application = Path.of(configuredApplication).toRealPath();
        if (!application.equals(root) && !application.startsWith(root.resolve("versions"))) throw new IOException("应用目录不属于此安装");
        if (!pending.toRealPath().equals(pending) || !pending.getParent().equals(root.resolve(".updates"))) {
            throw new IOException("更新目录归属不符");
        }
        String version = Files.readString(pending.resolve("version.txt")).trim();
        JsonNode manifest = ClientUpdatePackage.verifyManifest(Files.readAllBytes(pending.resolve("update-manifest.json")), version);
        // The external installer re-hashes the archive before waiting for exit. Do not hash a
        // hundreds-of-MB file on the FX confirmation callback.
        Path java = application.resolve("runtime/bin/javaw.exe");
        if (!Files.isRegularFile(java)) throw new IOException("本包缺少更新器 Java 运行时");
        String classpath = application.resolve("app/dhxy-client.jar") + ";" + application.resolve("app/lib/*");
        return new ProcessBuilder(java.toString(), "-cp", classpath, ClientUpdateInstaller.class.getName(),
                root.toString(), application.toString(), pending.toString(), Long.toString(ProcessHandle.current().pid()))
                .directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(pending.resolve("installer.log").toFile()).start();
    }

    private Path checkedRoot() throws Exception {
        if (!installationAvailable()) throw new IOException("开发环境不能自动替换，请使用正式安装包");
        Path root = Path.of(configuredRoot).toAbsolutePath().normalize();
        if (!root.toRealPath().equals(root) || !Files.isRegularFile(root.resolve("Start-DHXY.ps1"), LinkOption.NOFOLLOW_LINKS) ||
                !Files.isRegularFile(root.resolve("build-info.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("安装根目录不完整或包含链接");
        }
        if (json.readTree(root.resolve("build-info.json").toFile()).path("dirty").asBoolean(true)) {
            throw new IOException("本地未验收开发包不允许自动替换");
        }
        return root;
    }

    private void downloadFile(URI uri, Path target, long maximumBytes) throws Exception {
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null ||
                    (uri.getPort() != -1 && uri.getPort() != 443) ||
                    !isTrustedDownloadHost(uri.getHost())) {
                throw new IOException("更新下载跳转到了未授权地址");
            }
            HttpResponse<InputStream> response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/octet-stream").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (Set.of(301, 302, 303, 307, 308).contains(response.statusCode())) {
                    uri = uri.resolve(response.headers().firstValue("Location").orElseThrow());
                    continue;
                }
                if (response.statusCode() != 200 || response.headers().firstValueAsLong("Content-Length").orElse(0) > maximumBytes) {
                    throw new IOException("下载不可用或文件超限（HTTP " + response.statusCode() + "）");
                }
                // HttpRequest timeout does not bound an InputStream body; close it at the hard
                // deadline even if the server stalls between chunks.
                try (var deadline = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().factory())) {
                    var timeout = deadline.schedule(() -> { try { body.close(); } catch (IOException ignored) { } }, 60, TimeUnit.MINUTES);
                    try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        long total = 0;
                        byte[] buffer = new byte[65_536];
                        int count;
                        while ((count = body.read(buffer)) != -1) {
                            total += count;
                            if (total > maximumBytes) throw new IOException("更新下载超过声明大小");
                            output.write(buffer, 0, count);
                        }
                    } finally { timeout.cancel(false); }
                }
                return;
            }
        }
        throw new IOException("更新下载跳转次数过多");
    }

    /** Request the exact owner-recommended object without requiring a runtime task session. */
    private UpdateTicket requestUpdateTicket(String targetVersion) throws Exception {
        String licenseCode = credentialStore.load().orElseThrow(() ->
                new IOException("本机尚未保存更新授权，请先在说明页成功验证一次授权码"));
        URI base = URI.create(workerBaseUrl);
        if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null ||
                base.getQuery() != null || base.getFragment() != null) throw new IOException("更新授权服务地址无效");
        var body = json.createObjectNode();
        body.put("appId", "dhxy");
        body.put("currentVersion", currentVersion);
        body.put("targetVersion", targetVersion);
        body.put("licenseCode", licenseCode);
        body.put("deviceFingerprint", deviceFingerprintService.getDeviceFingerprint());
        body.put("clientId", runtimeClientId);
        body.put("clientToken", runtimeClientToken);
        HttpRequest request = HttpRequest.newBuilder(base.resolve("/api/client/update-ticket"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body))).build();
        HttpResponse<byte[]> response = http.send(request, new BoundedHttpBodyHandler(262_144));
        JsonNode root = json.readTree(response.body());
        if (response.statusCode() != 200 || !root.path("ok").asBoolean(false)) {
            throw new IOException(root.path("message").asText("更新下载未获授权，请重新验证授权"));
        }
        JsonNode data = root.path("data");
        if (!targetVersion.equals(data.path("version").asText())) throw new IOException("更新票据版本不匹配");
        byte[] manifestBytes;
        try { manifestBytes = Base64.getDecoder().decode(data.path("manifestBase64").asText()); }
        catch (IllegalArgumentException invalid) { throw new IOException("更新票据清单无效", invalid); }
        if (manifestBytes.length <= 0 || manifestBytes.length > 65_536) throw new IOException("更新清单大小无效");
        // Verify before trusting packageFile, size or hash fields carried by the ticket.
        ClientUpdatePackage.verifyManifest(manifestBytes, targetVersion);
        URI downloadUri = URI.create(data.path("downloadUrl").asText());
        if (!isR2DownloadHost(downloadUri.getHost()) || !"https".equals(downloadUri.getScheme()) ||
                downloadUri.getUserInfo() != null || downloadUri.getFragment() != null ||
                (downloadUri.getPort() != -1 && downloadUri.getPort() != 443)) {
            throw new IOException("更新票据下载地址无效");
        }
        return new UpdateTicket(manifestBytes, downloadUri);
    }

    private static boolean isTrustedDownloadHost(String host) {
        return Set.of("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com").contains(host) ||
                isR2DownloadHost(host);
    }

    private static boolean isR2DownloadHost(String host) {
        return host != null && host.matches("[a-f0-9]{32}\\.r2\\.cloudflarestorage\\.com");
    }

    private record UpdateTicket(byte[] manifestBytes, URI downloadUri) { }
}
