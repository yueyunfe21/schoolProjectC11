package com.bot.dhxy.cloud.turn;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves one Cloud-addressed PNG into the existing local template tree.
 *
 * <p>Each invocation handles only the supplied template key. It neither scans the template directory nor
 * refreshes content in the background. A missing or stale entry causes exactly one conditional download.</p>
 */
public final class TurnTemplateCache {

    private static final String WIRE_ROOT = "images/template/";
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final Pattern CONTENT_HASH = Pattern.compile("[0-9a-fA-F]{64}");

    private final Path templateRoot;
    private final TurnClient turnClient;

    /**
     * Creates an on-demand cache rooted at the existing local {@code images/template} directory.
     *
     * @param templateRoot existing local template root; paths returned by this cache never escape it
     * @param turnClient authenticated Cloud template transport; non-null
     */
    public TurnTemplateCache(Path templateRoot, TurnClient turnClient) {
        this.templateRoot = requireExistingRoot(templateRoot);
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
    }

    /**
     * Resolves one exact wire template version, downloading it once only when the local SHA-256 is stale.
     *
     * @param templateKey canonical wire key beginning with {@code images/template/} and ending in {@code .png}
     * @param contentHash target SHA-256 as the protocol's 64 hexadecimal characters
     * @return local PNG path under the configured template root
     * @throws TurnTransportException when the key/hash is invalid, Cloud is unavailable, content is invalid,
     *                                or the cache cannot be replaced atomically
     */
    public Path resolveTemplate(String templateKey, String contentHash) throws TurnTransportException {
        String checkedKey = requireTemplateKey(templateKey);
        String targetHash = requireContentHash(contentHash);
        Path target = resolveTarget(checkedKey);

        String localHash = hashRegularFileIfPresent(target);
        if (targetHash.equals(localHash)) {
            requireUsableLocalPng(target);
            return target;
        }

        String ifNoneMatch = localHash == null ? null : etag(localHash);
        TurnTemplateDownload download = turnClient.downloadTemplate(checkedKey, ifNoneMatch);
        if (download == null) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTRACT,
                    "template transport returned no typed download result");
        }
        if (download.status() == TurnTemplateDownload.Status.NOT_MODIFIED_304) {
            String currentHash = hashRegularFileIfPresent(target);
            if (!targetHash.equals(currentHash)) {
                throw new TurnTransportException(
                        TurnTransportException.Kind.TEMPLATE_HASH_MISMATCH,
                        "304 cannot satisfy the requested template hash because the current local PNG is missing or stale");
            }
            requireUsableLocalPng(target);
            return target;
        }

        byte[] png = download.pngBytes();
        if (download.status() != TurnTemplateDownload.Status.OK_200 || png == null) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTRACT,
                    "template download must be a typed 200 with PNG bytes or a validated 304");
        }
        String downloadedHash = sha256(png);
        if (!targetHash.equals(download.sha256()) || !targetHash.equals(downloadedHash)) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.TEMPLATE_HASH_MISMATCH,
                    "downloaded template does not match the requested contentHash");
        }
        requireDecodablePng(png);
        replaceAtomically(target, png);

        String installedHash = hashRegularFileIfPresent(target);
        if (!targetHash.equals(installedHash)) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.TEMPLATE_HASH_MISMATCH,
                    "installed template no longer matches the requested contentHash");
        }
        requireUsableLocalPng(target);
        return target;
    }

    private Path resolveTarget(String templateKey) throws TurnTransportException {
        String relativeKey = templateKey.substring(WIRE_ROOT.length());
        Path target = templateRoot.resolve(relativeKey.replace('/', java.io.File.separatorChar))
                .toAbsolutePath()
                .normalize();
        if (!target.startsWith(templateRoot) || target.equals(templateRoot)) {
            throw requestContract("templateKey escapes the configured template root");
        }
        verifyExistingAncestor(target.getParent());
        return target;
    }

    private void verifyExistingAncestor(Path directory) throws TurnTransportException {
        Path existing = directory;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            throw cacheFailure("template target has no existing ancestor under the configured root", null);
        }
        try {
            if (!existing.toRealPath().startsWith(templateRoot)) {
                throw requestContract("templateKey resolves outside the configured template root");
            }
        } catch (IOException e) {
            throw cacheFailure("cannot verify the template target path", e);
        }
    }

    private String hashRegularFileIfPresent(Path target) throws TurnTransportException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw cacheFailure("template target must be a regular non-symbolic file", null);
        }
        try (InputStream input = Files.newInputStream(target)) {
            MessageDigest digest = sha256Digest();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw cacheFailure("cannot read the local template cache entry", e);
        }
    }

    private void replaceAtomically(Path target, byte[] png) throws TurnTransportException {
        Path parent = target.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            Path realParent = parent.toRealPath();
            if (!realParent.startsWith(templateRoot)) {
                throw requestContract("template parent resolves outside the configured template root");
            }
            temporary = Files.createTempFile(realParent, ".turn-template-", ".png.tmp");
            Files.write(temporary, png, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw cacheFailure("template filesystem does not support atomic replacement", e);
        } catch (IOException e) {
            throw cacheFailure("cannot atomically replace the local template cache entry", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The typed installation failure remains authoritative; no background cleanup is scheduled.
                }
            }
        }
    }

    private static String requireTemplateKey(String templateKey) throws TurnTransportException {
        if (templateKey == null || templateKey.isBlank() || !templateKey.equals(templateKey.trim())) {
            throw requestContract("templateKey must be non-blank without surrounding whitespace");
        }
        if (!templateKey.startsWith(WIRE_ROOT) || !templateKey.endsWith(".png")) {
            throw requestContract("templateKey must use images/template/...png");
        }
        if (templateKey.indexOf('\\') >= 0 || templateKey.indexOf('%') >= 0 || templateKey.indexOf(':') >= 0
                || templateKey.indexOf('?') >= 0 || templateKey.indexOf('#') >= 0 || templateKey.indexOf('\0') >= 0) {
            throw requestContract("templateKey contains a forbidden path or encoding character");
        }
        String[] segments = templateKey.split("/", -1);
        if (segments.length < 3 || !"images".equals(segments[0]) || !"template".equals(segments[1])) {
            throw requestContract("templateKey must contain a PNG below images/template");
        }
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || !segment.equals(segment.trim())) {
                throw requestContract("templateKey contains an invalid path segment");
            }
        }
        return templateKey;
    }

    private static String requireContentHash(String contentHash) throws TurnTransportException {
        if (contentHash == null || !contentHash.equals(contentHash.trim())) {
            throw requestContract("contentHash must be a SHA-256 hexadecimal value without whitespace");
        }
        if (!CONTENT_HASH.matcher(contentHash).matches()) {
            throw requestContract("contentHash must be 64 hexadecimal characters");
        }
        return contentHash.toLowerCase(Locale.ROOT);
    }

    private static void requireDecodablePng(byte[] png) throws TurnTransportException {
        if (png.length < PNG_SIGNATURE.length) {
            throw responseContract("downloaded template is not a PNG byte stream", null);
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (png[i] != PNG_SIGNATURE[i]) {
                throw responseContract("downloaded template is not a PNG byte stream", null);
            }
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(png)) {
            var image = ImageIO.read(input);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw responseContract("downloaded template is not a decodable PNG image", null);
            }
        } catch (IOException e) {
            throw responseContract("downloaded template PNG cannot be decoded", e);
        }
    }

    private static void requireUsableLocalPng(Path target) throws TurnTransportException {
        try (InputStream input = Files.newInputStream(target)) {
            byte[] signature = input.readNBytes(PNG_SIGNATURE.length);
            if (signature.length != PNG_SIGNATURE.length) {
                throw cacheFailure("local template is not a PNG byte stream", null);
            }
            for (int i = 0; i < PNG_SIGNATURE.length; i++) {
                if (signature[i] != PNG_SIGNATURE[i]) {
                    throw cacheFailure("local template is not a PNG byte stream", null);
                }
            }
        } catch (IOException e) {
            throw cacheFailure("cannot read the local template PNG", e);
        }
        try {
            var image = ImageIO.read(target.toFile());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw cacheFailure("local template is not a decodable PNG image", null);
            }
        } catch (IOException e) {
            throw cacheFailure("cannot decode the local template PNG", e);
        }
    }

    private static String sha256(byte[] bytes) throws TurnTransportException {
        return HexFormat.of().formatHex(sha256Digest().digest(bytes));
    }

    private static MessageDigest sha256Digest() throws TurnTransportException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.INVALID_CONFIGURATION,
                    "SHA-256 is unavailable",
                    e);
        }
    }

    private static Path requireExistingRoot(Path root) {
        Objects.requireNonNull(root, "templateRoot");
        try {
            Path realRoot = root.toRealPath();
            if (!Files.isDirectory(realRoot, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("templateRoot must be an existing directory");
            }
            return realRoot;
        } catch (IOException e) {
            throw new IllegalArgumentException("templateRoot must be an accessible existing directory", e);
        }
    }

    private static String etag(String sha256) {
        return "\"sha256:" + sha256.toLowerCase(Locale.ROOT) + "\"";
    }

    private static TurnTransportException requestContract(String message) {
        return new TurnTransportException(TurnTransportException.Kind.REQUEST_CONTRACT, message);
    }

    private static TurnTransportException responseContract(String message, Throwable cause) {
        return new TurnTransportException(TurnTransportException.Kind.RESPONSE_CONTRACT, message, cause);
    }

    private static TurnTransportException cacheFailure(String message, Throwable cause) {
        return new TurnTransportException(TurnTransportException.Kind.INVALID_CONFIGURATION, message, cause);
    }
}
