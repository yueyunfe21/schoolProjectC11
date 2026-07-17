package com.bot.dhxy.cloud.turn;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Builds the exact two-part metadata JSON plus raw PNG multipart body used by the turn endpoint. */
public final class TurnMultipartBody {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_BOUNDARY_ATTEMPTS = 8;

    private final String contentType;
    private final byte[] body;

    private TurnMultipartBody(String boundary, byte[] body) {
        this.contentType = "multipart/form-data; boundary=" + boundary;
        this.body = body;
    }

    /**
     * Builds a bounded in-memory multipart body without transforming the PNG bytes.
     *
     * @param metadataJson serialized UTF-8 TurnRequest JSON; non-empty
     * @param pngBytes raw PNG bytes; non-empty
     * @return exact metadata/frame multipart body
     */
    public static TurnMultipartBody create(byte[] metadataJson, byte[] pngBytes) {
        Objects.requireNonNull(metadataJson, "metadataJson");
        Objects.requireNonNull(pngBytes, "pngBytes");
        if (metadataJson.length == 0 || pngBytes.length == 0) {
            throw new IllegalArgumentException("metadata JSON and PNG must be non-empty");
        }

        String boundary = chooseBoundary(metadataJson, pngBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream(metadataJson.length + pngBytes.length + 512);
        writeAscii(output, "--" + boundary + "\r\n");
        writeAscii(output, "Content-Disposition: form-data; name=\"metadata\"\r\n");
        writeAscii(output, "Content-Type: application/json\r\n\r\n");
        output.writeBytes(metadataJson);
        output.writeBytes(CRLF);
        writeAscii(output, "--" + boundary + "\r\n");
        writeAscii(output, "Content-Disposition: form-data; name=\"frame\"; filename=\"frame.png\"\r\n");
        writeAscii(output, "Content-Type: image/png\r\n\r\n");
        output.writeBytes(pngBytes);
        output.writeBytes(CRLF);
        writeAscii(output, "--" + boundary + "--\r\n");
        return new TurnMultipartBody(boundary, output.toByteArray());
    }

    public String contentType() {
        return contentType;
    }

    public long contentLength() {
        return body.length;
    }

    public HttpRequest.BodyPublisher bodyPublisher() {
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private static String chooseBoundary(byte[] metadataJson, byte[] pngBytes) {
        for (int attempt = 0; attempt < MAX_BOUNDARY_ATTEMPTS; attempt++) {
            String boundary = "dhxy-turn-" + UUID.randomUUID().toString().replace("-", "");
            byte[] marker = boundary.getBytes(StandardCharsets.US_ASCII);
            if (!contains(metadataJson, marker) && !contains(pngBytes, marker)) {
                return boundary;
            }
        }
        throw new IllegalStateException("unable to choose a collision-free multipart boundary");
    }

    private static boolean contains(byte[] body, byte[] marker) {
        outer:
        for (int i = 0; i <= body.length - marker.length; i++) {
            for (int j = 0; j < marker.length; j++) {
                if (body[i + j] != marker[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }
}
