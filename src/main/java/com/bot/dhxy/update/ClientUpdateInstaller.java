package com.bot.dhxy.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Separate installer entry, deliberately never starts Spring or game services. Its classpath and
 * runtime come from the already installed version, not downloaded code. Keeps every old version.
 */
public final class ClientUpdateInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(ClientUpdateInstaller.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long MAX_EXPANDED = 4L * 1024 * 1024 * 1024;

    private ClientUpdateInstaller() { }

    /**
     * @param args exact absolute install root, current app root, pending directory, and parent PID.
     * No shell fragments accepted. A live parent or occupied installation lock aborts without switching.
     */
    public static void main(String[] args) {
        Path pending = null;
        try {
            if (args.length != 4) throw new IOException("Invalid installer arguments");
            Path root = Path.of(args[0]).toAbsolutePath().normalize();
            Path previousApplication = Path.of(args[1]).toAbsolutePath().normalize();
            pending = Path.of(args[2]).toAbsolutePath().normalize();
            if (!root.toRealPath().equals(root) || !pending.toRealPath().equals(pending) ||
                    !pending.getParent().equals(root.resolve(".updates")) ||
                    !previousApplication.toRealPath().equals(previousApplication) ||
                    (!previousApplication.equals(root) && !previousApplication.startsWith(root.resolve("versions")))) {
                throw new IOException("Installer path ownership mismatch");
            }
            if (Files.size(pending.resolve("version.txt")) > 100 || Files.size(pending.resolve("update-manifest.json")) > 65_536) {
                throw new IOException("Update metadata oversized");
            }
            String version = Files.readString(pending.resolve("version.txt")).trim();
            JsonNode manifest = ClientUpdatePackage.verifyManifest(Files.readAllBytes(pending.resolve("update-manifest.json")), version);
            Path archive = pending.resolve(manifest.path("packageFile").asText());
            ClientUpdatePackage.verifyArchive(archive, manifest);
            ProcessHandle parent = ProcessHandle.of(Long.parseLong(args[3])).orElse(null);
            status(pending, "WAITING_FOR_EXIT", "等待客户端完全退出");
            if (parent != null && parent.isAlive()) parent.onExit().get(120, TimeUnit.SECONDS);
            Path pointer = root.resolve("current-version.json");
            byte[] previousPointer;
            Path nextApplication;
            try (FileChannel channel = FileChannel.open(root.resolve("client-session.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock ignored = acquire(channel)) {
                // The launcher's lock is held until both Java and its owned capture host exit.
                previousPointer = Files.exists(pointer) ? boundedPointer(pointer) : null;
                String activeRelative = previousPointer == null ? "." : JSON.readTree(previousPointer).path("application").asText();
                if (!root.resolve(activeRelative).normalize().equals(previousApplication)) {
                    throw new IOException("Active version changed while downloading; check for updates again");
                }
                Path versions = root.resolve("versions");
                Files.createDirectories(versions);
                if (!versions.toRealPath().equals(versions)) throw new IOException("Versions directory is a link");
                // Unique staging means a failed install is inspectable and never overwritten on retry.
                Path destination = Files.createDirectory(versions.resolve(manifest.path("candidate").asText() + "-" + pending.getFileName()));
                status(pending, "EXTRACTING", "解包到新目录，旧版保持不变");
                extract(archive, destination);
                nextApplication = destination.resolve("DHXY");
                validateApplication(nextApplication, manifest);
                for (String name : new String[]{"config", "data"}) {
                    copyUserTree(previousApplication.resolve(name), nextApplication.resolve(name));
                }
                String relative = root.relativize(nextApplication).toString().replace('\\', '/');
                byte[] nextPointer = JSON.writeValueAsBytes(Map.of("schemaVersion", 1, "application", relative, "version", version));
                if (previousPointer != null) Files.write(pending.resolve("previous-version.json"), previousPointer, StandardOpenOption.CREATE_NEW);
                Files.write(pending.resolve("next-version.json"), nextPointer, StandardOpenOption.CREATE_NEW);
                replacePointer(pointer, nextPointer);
                status(pending, "SWITCHED", "已切换版本，准备重新打开");
            }
            long startedAt = System.currentTimeMillis();
            Process launcher;
            try {
                launcher = launch(root);
            } catch (IOException startFailure) {
                restorePointer(root, pointer, pending, previousPointer);
                throw startFailure;
            }
            // Readiness is written only after Spring has shown the main UI. A timeout with a live
            // process is uncertain: do not kill it or roll back beneath an active application.
            for (int i = 0; i < 90; i++) {
                Path ready = root.resolve("update-ready.json");
                if (Files.isRegularFile(ready) && Files.size(ready) < 4096) {
                    try {
                        JsonNode receipt = JSON.readTree(ready.toFile());
                        if (version.equals(receipt.path("version").asText()) && receipt.path("startedAtMs").asLong() >= startedAt &&
                                ProcessHandle.of(receipt.path("pid").asLong()).map(ProcessHandle::isAlive).orElse(false)) {
                            status(pending, "COMPLETE", "新版窗口已打开；旧版保留");
                            return;
                        }
                    } catch (IOException partialWrite) { /* Next poll handles an in-progress receipt. */ }
                }
                if (!launcher.isAlive()) {
                    restorePointer(root, pointer, pending, previousPointer);
                    launch(root);
                    status(pending, "ROLLED_BACK", "新版未成功打开，已恢复旧版启动指针");
                    return;
                }
                Thread.sleep(1000);
            }
            status(pending, "STARTUP_UNCONFIRMED", "启动仍在进行，未收到窗口就绪；未强杀进程，旧版仍保留");
        } catch (Exception failure) {
            LOG.error("Client installation stopped; retain all versions and staging", failure);
            if (pending != null && Files.isDirectory(pending)) {
                try { status(pending, "FAILED", failure.getClass().getSimpleName() + ": " + failure.getMessage()); }
                catch (IOException ignored) { }
            }
            System.exit(1);
        }
    }

    private static FileLock acquire(FileChannel channel) throws Exception {
        for (int i = 0; i < 30; i++) {
            FileLock lock = channel.tryLock();
            if (lock != null) return lock;
            Thread.sleep(1000);
        }
        throw new IOException("Another client still holds the installation lock");
    }

    private static void extract(Path archive, Path destination) throws IOException {
        Set<String> targets = new HashSet<>();
        long total = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (++entries > 20_000 || !(name.equals("DHXY/") || name.startsWith("DHXY/")) ||
                        name.contains(":") || name.startsWith("/") || name.length() > 240) throw new IOException("Unsafe ZIP path");
                for (String part : name.split("/")) {
                    if (part.isEmpty() || part.equals(".") || part.equals("..") || part.endsWith(".") || part.endsWith(" ") ||
                            part.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) throw new IOException("Unsafe Windows ZIP name");
                }
                Path output = destination.resolve(name).normalize();
                if (!output.startsWith(destination) || !targets.add(name.toLowerCase(Locale.ROOT))) throw new IOException("Duplicate or escaping ZIP target");
                if (entry.isDirectory()) Files.createDirectories(output);
                else {
                    Files.createDirectories(output.getParent());
                    try (var file = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer = new byte[65_536];
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            total += count;
                            if (total > MAX_EXPANDED) throw new IOException("Expanded ZIP exceeds installation limit");
                            file.write(buffer, 0, count);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static void validateApplication(Path application, JsonNode manifest) throws IOException {
        for (String relative : new String[]{"DHXY.exe", "app/dhxy-client.jar", "runtime/bin/javaw.exe",
                "capture/dhxy-capture.exe", "build-info.json", "Start-DHXY.ps1", "Start-DHXY.cmd"}) {
            if (!Files.isRegularFile(application.resolve(relative), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Package incomplete: " + relative);
        }
        JsonNode build = JSON.readTree(application.resolve("build-info.json").toFile());
        if (!manifest.path("candidate").asText().equals(build.path("candidate").asText()) ||
                !manifest.path("sourceCommit").asText().equals(build.path("sourceCommit").asText()) ||
                !"dhxy-client".equals(build.path("product").asText()) || build.path("dirty").asBoolean(true)) {
            throw new IOException("Package build identity differs from signed manifest");
        }
    }

    private static void copyUserTree(Path source, Path destination) throws IOException {
        if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) return;
        long[] budget = {0, 0};
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.toRealPath().equals(dir.toAbsolutePath().normalize())) throw new IOException("User data contains a directory link");
                Files.createDirectories(destination.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile() || !file.toRealPath().equals(file.toAbsolutePath().normalize()) ||
                        ++budget[0] > 100_000 || (budget[1] += attrs.size()) > MAX_EXPANDED) throw new IOException("User data is linked, non-regular or too large");
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static byte[] boundedPointer(Path pointer) throws IOException {
        if (Files.size(pointer) > 4096 || !Files.isRegularFile(pointer, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid version pointer");
        return Files.readAllBytes(pointer);
    }

    private static void replacePointer(Path pointer, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(pointer.getParent(), "version-pointer-", ".tmp");
        Files.write(temporary, bytes);
        // No non-atomic fallback: a filesystem without atomic rename must retain the old pointer.
        Files.move(temporary, pointer, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void restorePointer(Path root, Path pointer, Path pending, byte[] previous) throws Exception {
        try (FileChannel channel = FileChannel.open(root.resolve("client-session.lock"), StandardOpenOption.WRITE);
                FileLock ignored = acquire(channel)) {
            if (!java.util.Arrays.equals(boundedPointer(pointer), Files.readAllBytes(pending.resolve("next-version.json")))) {
                throw new IOException("Version changed concurrently; do not roll it back");
            }
            // Initial installation has no pointer; explicit "." restores it without deleting anything.
            replacePointer(pointer, previous == null ? JSON.writeValueAsBytes(Map.of("schemaVersion", 1, "application", ".")) : previous);
        }
    }

    private static Process launch(Path root) throws IOException {
        Path powershell = Path.of(System.getenv("SystemRoot"), "System32/WindowsPowerShell/v1.0/powershell.exe");
        return new ProcessBuilder(powershell.toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-ExecutionPolicy", "Bypass", "-File", root.resolve("Start-DHXY.ps1").toString())
                .directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("update-launcher.log").toFile())).start();
    }

    private static void status(Path pending, String state, String message) throws IOException {
        Files.write(pending.resolve("installer-status.json"), JSON.writeValueAsBytes(Map.of(
                "state", state, "message", message == null ? "" : message, "at", Instant.now().toString())));
    }
}
