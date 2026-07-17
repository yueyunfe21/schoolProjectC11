package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnTemplateCacheContractTest {

    private static final String TEMPLATE_KEY = "images/template/turn-contract/frame.png";

    @TempDir
    Path temporaryDirectory;

    @Test
    void shaHitUsesExistingPngWithoutDownload() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        Path target = root.resolve("turn-contract/frame.png");
        Files.createDirectories(target.getParent());
        byte[] expected = fixturePng();
        Files.write(target, expected);
        RecordingTurnClient client = new RecordingTurnClient();

        Path resolved = new TurnTemplateCache(root, client).resolveTemplate(TEMPLATE_KEY, sha256(expected));

        assertEquals(target.toRealPath(), resolved.toRealPath());
        assertEquals(0, client.downloadCount);
        assertArrayEquals(expected, Files.readAllBytes(resolved));
    }

    @Test
    void missingEntryDownloadsTheExactHashOnceAndInstallsIt() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        byte[] expected = fixturePng();
        String hash = sha256(expected);
        RecordingTurnClient client = new RecordingTurnClient();
        client.enqueue(ok(expected));

        Path resolved = new TurnTemplateCache(root, client).resolveTemplate(TEMPLATE_KEY, hash);

        assertEquals(1, client.downloadCount);
        assertEquals(TEMPLATE_KEY, client.lastTemplateKey);
        assertEquals(null, client.lastIfNoneMatch);
        assertArrayEquals(expected, Files.readAllBytes(resolved));
        assertFalse(hasTemporaryInstallFiles(resolved.getParent()), "atomic install temp file must not remain");
    }

    @Test
    void staleEntrySendsItsEtagAndIsAtomicallyReplaced() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        Path target = root.resolve("turn-contract/frame.png");
        Files.createDirectories(target.getParent());
        byte[] stale = png(0xff112233, 0xff445566, 0xff778899, 0xffaabbcc);
        byte[] expected = fixturePng();
        Files.write(target, stale);
        RecordingTurnClient client = new RecordingTurnClient();
        client.enqueue(ok(expected));

        Path resolved = new TurnTemplateCache(root, client).resolveTemplate(TEMPLATE_KEY, sha256(expected));

        assertEquals(1, client.downloadCount);
        assertEquals(etag(sha256(stale)), client.lastIfNoneMatch);
        assertArrayEquals(expected, Files.readAllBytes(resolved));
        assertFalse(Arrays.equals(stale, Files.readAllBytes(resolved)));
        assertFalse(hasTemporaryInstallFiles(resolved.getParent()), "replacement must leave no partial file");
    }

    @Test
    void invalidDownloadedPngLeavesTheExistingCacheUntouched() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        Path target = root.resolve("turn-contract/frame.png");
        Files.createDirectories(target.getParent());
        byte[] stale = fixturePng();
        byte[] invalid = "not-a-png".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(target, stale);
        RecordingTurnClient client = new RecordingTurnClient();
        client.enqueue(ok(invalid));

        TurnTransportException failure = assertThrows(
                TurnTransportException.class,
                () -> new TurnTemplateCache(root, client).resolveTemplate(TEMPLATE_KEY, sha256(invalid)));

        assertEquals(TurnTransportException.Kind.RESPONSE_CONTRACT, failure.kind());
        assertEquals(1, client.downloadCount);
        assertEquals(etag(sha256(stale)), client.lastIfNoneMatch);
        assertArrayEquals(stale, Files.readAllBytes(target), "failed refresh must preserve the prior PNG");
        assertFalse(hasTemporaryInstallFiles(target.getParent()), "failed refresh must not leak a partial file");
    }

    @Test
    void rejectsPathTraversalBeforeCallingCloud() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        RecordingTurnClient client = new RecordingTurnClient();

        TurnTransportException failure = assertThrows(
                TurnTransportException.class,
                () -> new TurnTemplateCache(root, client).resolveTemplate(
                        "images/template/../outside.png",
                        "0".repeat(64)));

        assertEquals(TurnTransportException.Kind.REQUEST_CONTRACT, failure.kind());
        assertEquals(0, client.downloadCount);
        assertFalse(Files.exists(temporaryDirectory.resolve("outside.png")));
    }

    private static TurnTemplateDownload ok(byte[] png) throws Exception {
        String hash = sha256(png);
        return new TurnTemplateDownload(TurnTemplateDownload.Status.OK_200, etag(hash), hash, png);
    }

    private static String etag(String hash) {
        return "\"sha256:" + hash + "\"";
    }

    private static boolean hasTemporaryInstallFiles(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.anyMatch(path -> path.getFileName().toString().startsWith(".turn-template-"));
        }
    }

    private static byte[] fixturePng() throws IOException {
        try (var input = TurnTemplateCacheContractTest.class.getResourceAsStream("/cloud-turn/v1/frame-2x2.png")) {
            if (input == null) {
                throw new IOException("missing frame-2x2.png fixture");
            }
            return input.readAllBytes();
        }
    }

    private static byte[] png(int topLeft, int topRight, int bottomLeft, int bottomRight) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, topLeft);
        image.setRGB(1, 0, topRight);
        image.setRGB(0, 1, bottomLeft);
        image.setRGB(1, 1, bottomRight);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "png", output));
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static final class RecordingTurnClient implements TurnClient {
        private final Deque<TurnTemplateDownload> downloads = new ArrayDeque<>();
        private int downloadCount;
        private String lastTemplateKey;
        private String lastIfNoneMatch;

        private void enqueue(TurnTemplateDownload download) {
            downloads.add(download);
        }

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
            throw new AssertionError("turn exchange is outside the template-cache contract");
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            downloadCount++;
            lastTemplateKey = templateKey;
            lastIfNoneMatch = ifNoneMatch;
            if (downloads.isEmpty()) {
                throw new AssertionError("unexpected template download");
            }
            return downloads.removeFirst();
        }
    }
}
