package com.bot.dhxy.cloud.turn;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable conditional template response with an exact ETag-derived SHA-256 identity.
 *
 * @param status typed HTTP success status
 * @param etag exact quoted ETag returned by Cloud
 * @param sha256 lowercase SHA-256 hex represented by the ETag
 * @param pngBytes raw PNG bytes for 200, or null for 304
 */
public record TurnTemplateDownload(
        Status status,
        String etag,
        String sha256,
        byte[] pngBytes) {

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern SHA256_ETAG = Pattern.compile("\\\"sha256:([0-9a-f]{64})\\\"");

    public TurnTemplateDownload {
        Objects.requireNonNull(status, "status");
        if (etag == null || etag.isBlank()) {
            throw new IllegalArgumentException("etag must be non-blank");
        }
        if (sha256 == null || !SHA256.matcher(sha256.toLowerCase(Locale.ROOT)).matches()) {
            throw new IllegalArgumentException("sha256 must be 64 lowercase hexadecimal characters");
        }
        sha256 = sha256.toLowerCase(Locale.ROOT);
        var etagMatcher = SHA256_ETAG.matcher(etag);
        if (!etagMatcher.matches() || !etagMatcher.group(1).equals(sha256)) {
            throw new IllegalArgumentException("etag must represent the same lowercase SHA-256");
        }
        if (status == Status.OK_200 && (pngBytes == null || pngBytes.length == 0)) {
            throw new IllegalArgumentException("OK_200 requires PNG bytes");
        }
        if (status == Status.NOT_MODIFIED_304 && pngBytes != null) {
            throw new IllegalArgumentException("NOT_MODIFIED_304 must not contain PNG bytes");
        }
        pngBytes = pngBytes == null ? null : pngBytes.clone();
    }

    @Override
    public byte[] pngBytes() {
        return pngBytes == null ? null : pngBytes.clone();
    }

    public int httpStatus() {
        return status.httpStatus;
    }

    public enum Status {
        OK_200(200),
        NOT_MODIFIED_304(304);

        private final int httpStatus;

        Status(int httpStatus) {
            this.httpStatus = httpStatus;
        }
    }
}
