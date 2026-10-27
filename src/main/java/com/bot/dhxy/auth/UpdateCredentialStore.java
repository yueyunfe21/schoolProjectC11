package com.bot.dhxy.auth;

import com.sun.jna.platform.win32.Crypt32Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

/** Stores the already-verified update credential with Windows-user DPAPI protection. */
@Service
public class UpdateCredentialStore {
    private static final String PREFIX = "dhxy-update-authorization-v1\n";
    private static final int MAX_FILE_BYTES = 16_384;
    private final String configuredRoot;

    public UpdateCredentialStore(@Value("${dhxy.install-root:}") String configuredRoot) {
        this.configuredRoot = configuredRoot;
    }

    /**
     * Persist a license only after the Worker has confirmed this exact device.
     * @param licenseCode verified non-blank license code; plaintext never reaches the file system.
     */
    public void save(String licenseCode) throws IOException {
        if (licenseCode == null || licenseCode.isBlank()) throw new IOException("更新授权为空");
        Path target = credentialPath(true);
        byte[] clear = (PREFIX + licenseCode.trim()).getBytes(StandardCharsets.UTF_8);
        byte[] protectedBytes;
        try { protectedBytes = Crypt32Util.cryptProtectData(clear); }
        catch (RuntimeException failure) { throw new IOException("无法使用 Windows 保护更新授权", failure); }
        Path temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporary, protectedBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
            java.util.Arrays.fill(clear, (byte) 0);
            java.util.Arrays.fill(protectedBytes, (byte) 0);
        }
    }

    /**
     * Load the credential for an explicit user-triggered update request.
     * @return decrypted license for this Windows user, or empty when no prior verified login exists.
     */
    public Optional<String> load() throws IOException {
        Path path = credentialPath(false);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        long size = Files.size(path);
        if (size <= 0 || size > MAX_FILE_BYTES) throw new IOException("本机更新授权文件无效，请重新验证授权");
        byte[] protectedBytes = Files.readAllBytes(path);
        byte[] clear;
        try { clear = Crypt32Util.cryptUnprotectData(protectedBytes); }
        catch (RuntimeException failure) { throw new IOException("本机更新授权无法解密，请重新验证授权", failure); }
        try {
            String value = new String(clear, StandardCharsets.UTF_8);
            if (!value.startsWith(PREFIX) || value.length() <= PREFIX.length()) {
                throw new IOException("本机更新授权格式无效，请重新验证授权");
            }
            return Optional.of(value.substring(PREFIX.length()).trim());
        } finally {
            java.util.Arrays.fill(clear, (byte) 0);
            java.util.Arrays.fill(protectedBytes, (byte) 0);
        }
    }

    private Path credentialPath(boolean createDirectory) throws IOException {
        if (configuredRoot == null || configuredRoot.isBlank() ||
                !System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            throw new IOException("只有正式 Windows 安装包才能保存更新授权");
        }
        Path root = Path.of(configuredRoot).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("安装根目录不存在");
        Path config = root.resolve("config");
        if (createDirectory) Files.createDirectories(config);
        if (Files.exists(config, LinkOption.NOFOLLOW_LINKS) &&
                (!Files.isDirectory(config, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(config))) {
            throw new IOException("配置目录不安全");
        }
        Path result = config.resolve("update-authorization.dat").normalize();
        if (!result.startsWith(root) || Files.isSymbolicLink(result)) throw new IOException("更新授权路径不安全");
        return result;
    }
}
