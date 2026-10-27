package com.bot.dhxy.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Trust boundary shared by the downloader and the separate installer. Only signed raw payload
 * bytes authorize installation; public version announcements alone never authorize execution.
 */
public final class ClientUpdatePackage {
    public static final long MAX_PACKAGE_BYTES = 1_073_741_824L;
    public static final String RELEASE_BASE = "https://github.com/yueyunfe21/schoolProjectC11/releases/download/";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ClientUpdatePackage() { }

    /**
     * @param envelopeBytes bounded UTF-8 envelope downloaded without authentication credentials.
     * @param expectedVersion exact version requested by the user from the policy announcement.
     * @return verified payload; never use fields from an envelope before this returns.
     */
    public static JsonNode verifyManifest(byte[] envelopeBytes, String expectedVersion) throws Exception {
        if (envelopeBytes.length > 65_536 || expectedVersion == null || expectedVersion.length() > 100 ||
                !expectedVersion.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)-b[1-9][0-9]*\\.[1-9][0-9]*")) {
            throw new IOException("Invalid update identity");
        }
        JsonNode envelope = JSON.readTree(envelopeBytes);
        if (envelope.path("schemaVersion").asInt() != 1 ||
                !"SHA256withRSA".equals(envelope.path("algorithm").asText())) throw new IOException("Unsupported signature");
        byte[] payload = Base64.getDecoder().decode(envelope.path("payload").asText());
        byte[] signature = Base64.getDecoder().decode(envelope.path("signature").asText());
        try (InputStream stream = ClientUpdatePackage.class.getResourceAsStream("/update-public-key.pem")) {
            if (stream == null) throw new IOException("Release trust key missing");
            String pem = new String(stream.readNBytes(16_384), StandardCharsets.US_ASCII)
                    .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(payload);
            if (!verifier.verify(signature)) throw new IOException("Update publisher signature invalid");
        }
        JsonNode data = JSON.readTree(payload);
        String candidate = "client-" + expectedVersion;
        if (data.path("schemaVersion").asInt() != 1 ||
                !"dhxy-client".equals(data.path("product").asText()) || !"prod".equals(data.path("channel").asText()) ||
                !"windows-x64".equals(data.path("platform").asText()) ||
                !"yueyunfe21/schoolProjectC11".equals(data.path("repository").asText()) ||
                !expectedVersion.equals(data.path("version").asText()) || !candidate.equals(data.path("candidate").asText()) ||
                !(candidate + "-windows-x64.zip").equals(data.path("packageFile").asText()) ||
                !data.path("sha256").asText().matches("[a-f0-9]{64}") ||
                !data.path("sourceCommit").asText().matches("[a-f0-9]{40}") ||
                !data.path("sizeBytes").isIntegralNumber() || !data.path("sizeBytes").canConvertToLong() ||
                data.path("sizeBytes").asLong() <= 0 || data.path("sizeBytes").asLong() > MAX_PACKAGE_BYTES) {
            throw new IOException("Signed update metadata does not describe the requested product");
        }
        return data;
    }

    /** @param archive downloaded ZIP, not an extracted/installed directory; @param manifest verified payload. */
    public static void verifyArchive(Path archive, JsonNode manifest) throws Exception {
        if (Files.size(archive) != manifest.path("sizeBytes").asLong()) throw new IOException("Update size mismatch");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] buffer = new byte[65_536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(manifest.path("sha256").asText())) {
            throw new IOException("Update checksum mismatch");
        }
    }
}
