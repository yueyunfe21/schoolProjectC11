package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Encodes unscaled capture pixels as deterministic PNG frame payloads.
 */
@Component
public final class TurnPngCodec {

    private static final String PNG_CONTENT_TYPE = "image/png";

    /**
     * Encode one image and derive protocol metadata from those exact source pixels and PNG bytes.
     *
     * @param image captured image at one source pixel per output pixel; not retained after return.
     * @param purpose one of the protocol's closed frame purposes.
     * @param region screen-absolute region represented by the image.
     * @param sourceStepIndex zero-based source step index, or null for non-step failure evidence.
     * @return raw PNG plus metadata whose dimensions and SHA-256 match the encoded payload.
     */
    public TurnFrame encode(BufferedImage image,
                            TurnFramePurpose purpose,
                            TurnRegion region,
                            Integer sourceStepIndex) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(region, "region");
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("image dimensions must be positive");
        }
        if (region.width() != image.getWidth() || region.height() != image.getHeight()) {
            throw new IllegalArgumentException("frame region dimensions do not match captured pixels");
        }
        if (sourceStepIndex != null && sourceStepIndex < 0) {
            throw new IllegalArgumentException("sourceStepIndex must be nonnegative");
        }

        byte[] png = encodePng(image);
        TurnFrameMetadata metadata = new TurnFrameMetadata(
                purpose,
                PNG_CONTENT_TYPE,
                sha256(png),
                image.getWidth(),
                image.getHeight(),
                region,
                sourceStepIndex);
        return new TurnFrame(metadata, png);
    }

    private byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG ImageIO writer is unavailable");
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode turn capture as PNG", e);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
