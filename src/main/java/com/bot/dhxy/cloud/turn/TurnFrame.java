package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;

import java.util.Objects;

/**
 * One raw PNG frame and its protocol metadata.
 *
 * @param metadata metadata derived from the encoded pixels.
 * @param pngBytes raw PNG bytes for multipart transfer; never Base64 encoded.
 */
public record TurnFrame(TurnFrameMetadata metadata, byte[] pngBytes) {

    public TurnFrame {
        Objects.requireNonNull(metadata, "metadata");
        if (pngBytes == null || pngBytes.length == 0) {
            throw new IllegalArgumentException("pngBytes must not be empty");
        }
        pngBytes = pngBytes.clone();
    }

    @Override
    public byte[] pngBytes() {
        return pngBytes.clone();
    }
}
