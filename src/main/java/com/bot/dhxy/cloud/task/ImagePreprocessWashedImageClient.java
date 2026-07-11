package com.bot.dhxy.cloud.task;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class ImagePreprocessWashedImageClient {

    private static final String PNG_MIME_TYPE = "image/png";
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "image-preprocess";

    private final ImagePreprocessCloudService imagePreprocessCloudService;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    public ImagePreprocessWashedImageClient(
            ImagePreprocessCloudService imagePreprocessCloudService,
            TaskExecutionContextHolder taskExecutionContextHolder) {
        this.imagePreprocessCloudService = imagePreprocessCloudService;
        this.taskExecutionContextHolder = taskExecutionContextHolder;
    }

    /**
     * Sends an in-memory raw screenshot to `IMAGE_PREPROCESS` and decodes a validated washed PNG.
     *
     * @param raw source image in Java image coordinates; ownership remains with the caller.
     * @param operation wash operation requested from the cloud image-preprocess service.
     * @param metadata caller/task/window metadata used for traceability and safety bounds; nullable fields are allowed.
     * @return a result whose image is non-null only when cloud returned a gate-validated washed PNG.
     */
    public WashedImageResult wash(
            BufferedImage raw,
            ImagePreprocessOperation operation,
            RequestMetadata metadata) {
        if (raw == null || operation == null) {
            return new WashedImageResult(
                    ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                    null,
                    "missing raw image/operation",
                    null);
        }
        ImagePreprocessCloudDecision decision = imagePreprocessCloudService.preprocess(
                request(raw, operation, metadata, null));
        if (!decision.hasWashedImage()) {
            return new WashedImageResult(decision.getStatus(), null, decision.getReason(), decision);
        }
        try {
            return new WashedImageResult(
                    decision.getStatus(),
                    decodeWashedPng(decision),
                    decision.getReason(),
                    decision);
        } catch (IOException | IllegalArgumentException e) {
            return new WashedImageResult(
                    ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                    null,
                    "decode validated washed image failed: " + e.getMessage(),
                    decision);
        }
    }

    /**
     * Sends a raw PNG file to `IMAGE_PREPROCESS`, decodes the washed PNG, and writes it to disk.
     *
     * @param rawPath source PNG path; this is read into memory and sent as a raw PNG payload.
     * @param washedPath output path for the decoded washed PNG; parent directories are created when present.
     * @param operation wash operation requested from cloud.
     * @param metadata caller/task/window metadata used for traceability and safety bounds; nullable fields are allowed.
     * @return cloud status plus decoded image when available. The returned image remains caller-owned.
     */
    public WashedImageResult washToPath(
            Path rawPath,
            Path washedPath,
            ImagePreprocessOperation operation,
            RequestMetadata metadata) {
        try {
            BufferedImage raw = ImageIO.read(rawPath.toFile());
            if (raw == null) {
                return new WashedImageResult(
                        ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                        null,
                        "raw image unreadable: " + rawPath,
                        null);
            }
            try {
                RequestMetadata merged = metadata == null
                        ? RequestMetadata.builder().rawImagePath(pathText(rawPath)).build()
                        : metadata.toBuilder().rawImagePath(pathText(rawPath)).build();
                WashedImageResult result = wash(raw, operation, merged);
                if (result.hasImage()) {
                    writePng(result.image(), washedPath);
                }
                return result;
            } finally {
                raw.flush();
            }
        } catch (IOException e) {
            return new WashedImageResult(
                    ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                    null,
                    "raw image read/write failed: " + e.getMessage(),
                    null);
        }
    }

    private ImagePreprocessCloudRequest request(
            BufferedImage raw,
            ImagePreprocessOperation operation,
            RequestMetadata metadata,
            Path rawPath) {
        try {
            byte[] rawPng = encodePng(raw);
            TaskExecutionContext context = taskExecutionContextHolder.current().orElse(null);
            int windowWidth = context != null && context.hasNativeWindowGeometry()
                    ? context.getNativeWindowWidth()
                    : raw.getWidth();
            int windowHeight = context != null && context.hasNativeWindowGeometry()
                    ? context.getNativeWindowHeight()
                    : raw.getHeight();
            RequestMetadata safeMetadata = metadata == null ? RequestMetadata.builder().build() : metadata;
            return ImagePreprocessCloudRequest.builder()
                    .operation(operation)
                    .imagePayloadBase64(Base64.getEncoder().encodeToString(rawPng))
                    .payloadMimeType(PNG_MIME_TYPE)
                    .imageSha256(sha256Hex(rawPng))
                    .rawImagePath(firstText(safeMetadata.getRawImagePath(), pathText(rawPath)))
                    .debugImageId(safeMetadata.getDebugImageId())
                    .source(safeMetadata.getSource())
                    .taskCode(firstText(safeMetadata.getTaskCode(), context == null ? DEFAULT_TASK_CODE : context.getTaskCode()))
                    .phase(firstText(safeMetadata.getPhase(), DEFAULT_PHASE))
                    .windowId(firstText(safeMetadata.getWindowId(), context == null ? null : context.getWindowId()))
                    .taskRunId(firstText(safeMetadata.getTaskRunId(),
                            context == null ? null : Long.toString(context.getTaskRunId())))
                    .policyVersion(safeMetadata.getPolicyVersion())
                    .hwnd(firstText(safeMetadata.getHwnd(), context == null ? null : context.getNativeWindowHandle()))
                    .parameters(safeMetadata.getParameters())
                    .windowWidth(windowWidth)
                    .windowHeight(windowHeight)
                    .roi(ImagePreprocessCloudRequest.Roi.builder()
                            .x(0)
                            .y(0)
                            .width(Math.min(raw.getWidth(), windowWidth))
                            .height(Math.min(raw.getHeight(), windowHeight))
                            .build())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("encode image preprocess raw PNG payload failed", e);
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static BufferedImage decodeWashedPng(ImagePreprocessCloudDecision decision) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(decision.getWashedImagePayloadBase64());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("decoded washed payload is not a PNG");
        }
        return image;
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            throw new IOException("washed output path is a directory: " + path);
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("no PNG writer accepted output path: " + path);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String pathText(Path path) {
        return path == null ? "" : path.toString();
    }

    private static String firstText(String first, String second) {
        return Optional.ofNullable(first)
                .filter(value -> !value.isBlank())
                .orElse(second == null ? "" : second);
    }

    @Value
    @Builder(toBuilder = true)
    public static class RequestMetadata {
        String rawImagePath;
        String debugImageId;
        String source;
        String taskCode;
        String phase;
        String windowId;
        String taskRunId;
        String policyVersion;
        String hwnd;
        @Builder.Default
        Map<String, String> parameters = Map.of();
    }

    public record WashedImageResult(
            ImagePreprocessCloudDecision.Status status,
            BufferedImage image,
            String reason,
            ImagePreprocessCloudDecision decision) {

        public boolean hasImage() {
            return image != null;
        }
    }
}
