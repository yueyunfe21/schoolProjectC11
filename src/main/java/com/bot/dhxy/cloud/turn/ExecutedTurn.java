package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** One validated outcome and its optional raw PNG multipart body. */
public record ExecutedTurn(TurnOutcome outcome, byte[] optionalPng) {

    public ExecutedTurn {
        TurnProtocolValidator.requireValid(Objects.requireNonNull(outcome, "outcome"));
        boolean hasMetadata = outcome.frame() != null;
        boolean hasPng = optionalPng != null;
        if (hasMetadata != hasPng) {
            throw new IllegalArgumentException("outcome frame metadata and PNG must be both present or both absent");
        }
        if (hasPng) {
            if (optionalPng.length == 0) {
                throw new IllegalArgumentException("optionalPng must not be empty");
            }
            byte[] pngCopy = optionalPng.clone();
            requireMatchingPng(outcome.frame(), pngCopy);
            optionalPng = pngCopy;
        }
    }

    @Override
    public byte[] optionalPng() {
        return optionalPng == null ? null : optionalPng.clone();
    }

    private static void requireMatchingPng(TurnFrameMetadata metadata, byte[] png) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (png.length < signature.length) {
            throw new IllegalArgumentException("optionalPng is not a PNG image");
        }
        for (int index = 0; index < signature.length; index++) {
            if (png[index] != signature[index]) {
                throw new IllegalArgumentException("optionalPng is not a PNG image");
            }
        }
        String actualHash = sha256(png);
        if (!actualHash.equalsIgnoreCase(metadata.sha256())) {
            throw new IllegalArgumentException("outcome frame SHA-256 does not match optionalPng");
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(png)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("optionalPng is not a decodable PNG image");
            }
            try {
                if (image.getWidth() != metadata.width() || image.getHeight() != metadata.height()) {
                    throw new IllegalArgumentException("outcome frame dimensions do not match optionalPng");
                }
            } finally {
                image.flush();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("optionalPng cannot be decoded", e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
