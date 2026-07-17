package com.bot.dhxy.service.dialog;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * Whole local mechanics cohort for the committed {@code 696a12b0} story-objective capture/crop,
 * extracted from {@code DialogService.handleStoryObjective}/{@code cropStoryObjectiveImage},
 * {@code captureCurrentStoryImage}, {@code captureCurrentStoryObjectiveSnapshotNoDetect},
 * {@code cropStoryObjectiveFromWindowSnapshotNoDetect} and the story-objective debug rules.
 *
 * <p>The three closed operations only capture, crop and encode a typed result; this class never
 * recognizes objective text, never picks a target map/coordinate, never builds a {@code DialogResult}
 * and never decides fallback. It reuses read-only the existing {@link DialogDetectionLocalMechanics},
 * {@link BoundWindowCaptureService}, {@link ImagePreprocessor} pure-local crops/save and
 * {@link WindowScopedTempPath}. The baseline small rect, window-origin conversion, capture/crop/save
 * counts, latest+history debug rule and single-owner/flush per {@link BufferedImage} are preserved; no
 * local {@code Path} is returned as a cross-boundary authority. This chain has no input, runs outside
 * the input queue on an exact binding, and adds no retry/TTL/session/ledger/owner.</p>
 */
@Slf4j
@Service
public final class DialogStoryObjectiveCaptureLocalMechanics {

    // Committed dialog geometry (window-client pixels) from DialogService; screen-absolute rects add
    // only the window origin (no DPI scaling), matching the approved DialogDetectionLocalMechanics.
    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;
    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;

    private final BoundWindowCaptureService captureService;
    private final DialogDetectionLocalMechanics dialogDetectionLocalMechanics;
    private final WindowScopedTempPath windowScopedTempPath;

    public DialogStoryObjectiveCaptureLocalMechanics(BoundWindowCaptureService captureService,
                                                     DialogDetectionLocalMechanics dialogDetectionLocalMechanics,
                                                     WindowScopedTempPath windowScopedTempPath) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.dialogDetectionLocalMechanics = Objects.requireNonNull(
                dialogDetectionLocalMechanics, "dialogDetectionLocalMechanics");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /** Closed terminal for one story-objective operation. */
    public enum Status {
        CAPTURED,
        NOT_STORY,
        CAPTURE_UNAVAILABLE,
        CROP_FAILED,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * Immutable command for {@link #cropStoryObjectiveFromWindowSnapshot}: a full game-window snapshot in
     * window-local pixels plus its screen-absolute origin. The PNG bytes are defensively copied and must
     * be non-empty.
     */
    public record WindowSnapshotCropCommand(
            byte[] windowSnapshotPngBytes,
            int windowBaseX,
            int windowBaseY,
            String source) {

        public WindowSnapshotCropCommand {
            if (windowSnapshotPngBytes == null || windowSnapshotPngBytes.length == 0) {
                throw new IllegalArgumentException("windowSnapshotPngBytes must be non-empty");
            }
            windowSnapshotPngBytes = windowSnapshotPngBytes.clone();
        }

        @Override
        public byte[] windowSnapshotPngBytes() {
            return windowSnapshotPngBytes.clone();
        }
    }

    /**
     * Immutable closed result. Only {@link Status#CAPTURED} carries the objective PNG bytes, its
     * recomputed SHA-256, pixel dimensions and screen-absolute top-left; every other status has all of
     * those fields null. The compact constructor rejects any mixed shape and validates a captured frame.
     */
    public record StoryObjectiveResult(
            Status status,
            boolean fullFrameFallback,
            byte[] objectivePngBytes,
            String objectiveSha256,
            Integer width,
            Integer height,
            Integer absoluteLeft,
            Integer absoluteTop,
            String reason) {

        public StoryObjectiveResult {
            Objects.requireNonNull(status, "status");
            objectivePngBytes = objectivePngBytes == null ? null : objectivePngBytes.clone();
            boolean captured = status == Status.CAPTURED;
            boolean hasAllCore = objectivePngBytes != null && objectiveSha256 != null
                    && width != null && height != null && absoluteLeft != null && absoluteTop != null;
            boolean hasAnyCore = objectivePngBytes != null || objectiveSha256 != null
                    || width != null || height != null || absoluteLeft != null || absoluteTop != null;
            if (captured && !hasAllCore) {
                throw new IllegalArgumentException("CAPTURED result must carry all objective fields");
            }
            if (!captured && hasAnyCore) {
                throw new IllegalArgumentException("non-CAPTURED result must not carry any objective field");
            }
            if (!captured && fullFrameFallback) {
                throw new IllegalArgumentException("fullFrameFallback is only valid for a CAPTURED result");
            }
            if (captured) {
                // Independently decode the PNG and recompute SHA-256/dimensions from the actual bytes so
                // no untrusted canonical-constructor caller can build a self-contradictory image
                // authority; the declared values must match the bytes.
                verifyCapturedImageConsistency(objectivePngBytes, objectiveSha256, width, height);
            }
        }

        private static void verifyCapturedImageConsistency(byte[] pngBytes, String declaredSha256,
                                                           int declaredWidth, int declaredHeight) {
            if (pngBytes.length == 0 || declaredSha256.isBlank() || declaredWidth <= 0 || declaredHeight <= 0) {
                throw new IllegalArgumentException("invalid CAPTURED objective image, dimensions, or hash");
            }
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(pngBytes));
            } catch (IOException e) {
                throw new IllegalArgumentException("CAPTURED objective bytes are not a decodable image", e);
            }
            if (decoded == null) {
                throw new IllegalArgumentException("CAPTURED objective bytes are not a decodable image");
            }
            int actualWidth = decoded.getWidth();
            int actualHeight = decoded.getHeight();
            decoded.flush();
            if (actualWidth != declaredWidth || actualHeight != declaredHeight) {
                throw new IllegalArgumentException("declared dimensions do not match the objective image bytes");
            }
            String actualSha256;
            try {
                actualSha256 = sha256Hex(pngBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("cannot verify objective SHA-256", e);
            }
            if (!actualSha256.equals(declaredSha256)) {
                throw new IllegalArgumentException("declared SHA-256 does not match the objective image bytes");
            }
        }

        @Override
        public byte[] objectivePngBytes() {
            return objectivePngBytes == null ? null : objectivePngBytes.clone();
        }
    }

    /**
     * {@code DETECT_AND_CAPTURE_STORY_OBJECTIVE}: detect the dialog on the exact binding, accept only
     * {@code STORY}, and crop the small story rect from the very same detection frame (no second
     * capture), mirroring {@code handleStoryObjective}/{@code cropStoryObjectiveImage}.
     */
    public StoryObjectiveResult detectAndCaptureStoryObjective(WindowNativeBinding binding, String source) {
        String safeSource = safeSource(source);
        if (binding == null || !binding.hasNativeHandle()) {
            return terminal(Status.BINDING_UNAVAILABLE, "binding-unavailable source=" + safeSource);
        }
        DialogDetectionLocalMechanics.DialogDetectionResult detection =
                dialogDetectionLocalMechanics.detectDialog(binding, false, 0L, "story-objective:" + safeSource);
        switch (detection.state()) {
            case CAPTURED -> {
                if (detection.dialogType() != DialogType.STORY) {
                    return terminal(Status.NOT_STORY, "not-story type=" + detection.dialogType() + " source=" + safeSource);
                }
                byte[] frameBytes = detection.framePngBytes();
                if (frameBytes == null) {
                    return terminal(Status.MECHANICS_FAILED, "detection-frame-missing source=" + safeSource);
                }
                BufferedImage frame;
                try {
                    frame = ImageIO.read(new ByteArrayInputStream(frameBytes));
                } catch (IOException e) {
                    log.warn("story objective detection frame decode failed: source={} reason={}", safeSource, e.getMessage(), e);
                    return terminal(Status.MECHANICS_FAILED, "detection-frame-decode-failed source=" + safeSource);
                }
                if (frame == null) {
                    return terminal(Status.MECHANICS_FAILED, "detection-frame-null source=" + safeSource);
                }
                try {
                    // P1-1: derive BOTH rects from the SAME fresh detection large rect; never reuse the
                    // entry binding X/Y, which may be stale after detect wait/hide. The small rect is the
                    // large-rect origin plus the baseline client delta (SMALL-LARGE = (0, +33), 529x143).
                    int largeLeft = detection.dialogLeft();
                    int largeTop = detection.dialogTop();
                    int smallLeft = largeLeft + (DIALOG_SMALL_X - DIALOG_LARGE_X);
                    int smallTop = largeTop + (DIALOG_SMALL_Y - DIALOG_LARGE_Y);
                    int[] largeRect = {largeLeft, largeTop, detection.dialogRight(), detection.dialogBottom()};
                    int[] smallRect = {smallLeft, smallTop, smallLeft + DIALOG_SMALL_W, smallTop + DIALOG_SMALL_H};
                    // Reuse the SAME frame; crop only the small story rect, no re-capture.
                    BufferedImage cropped = ImagePreprocessor.cropAbsoluteRect(frame, largeRect, smallRect);
                    if (cropped == null) {
                        // P1-2: baseline cropStoryObjectiveImage returns the original detection frame when
                        // the small crop is null; reproduce as a full-frame fallback CAPTURED (no second
                        // capture) carrying the fresh large-rect absolute origin/size.
                        return capturedFrom(frame, largeLeft, largeTop, safeSource, true, false);
                    }
                    return capturedFrom(cropped, smallLeft, smallTop, safeSource, false, true);
                } finally {
                    frame.flush();
                }
            }
            case CAPTURE_UNAVAILABLE -> {
                return terminal(Status.CAPTURE_UNAVAILABLE, "detect-capture-unavailable source=" + safeSource);
            }
            case PRE_CAPTURE_INTERRUPTED -> {
                return terminal(Status.INTERRUPTED, "detect-pre-capture-interrupted source=" + safeSource);
            }
            case NON_INPUT_WORKER, MECHANICS_FAILED -> {
                return terminal(Status.MECHANICS_FAILED, "detect-mechanics-failed state=" + detection.state() + " source=" + safeSource);
            }
            default -> {
                return terminal(Status.MECHANICS_FAILED, "detect-unknown-state source=" + safeSource);
            }
        }
    }

    /**
     * {@code CAPTURE_STORY_OBJECTIVE_NO_DETECT}: capture the baseline large rect once on the exact
     * binding, then crop the small rect by baseline geometry without classifying the dialog, mirroring
     * {@code captureCurrentStoryObjectiveSnapshotNoDetect}.
     */
    public StoryObjectiveResult captureStoryObjectiveNoDetect(WindowNativeBinding binding, String source) {
        String safeSource = safeSource(source);
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return terminal(Status.BINDING_UNAVAILABLE, "binding-unavailable source=" + safeSource);
        }
        int[] largeRect = largeScreenRect(binding);
        int[] smallRect = smallScreenRect(binding);
        if (largeRect == null || smallRect == null) {
            return terminal(Status.MECHANICS_FAILED, "rect-overflow source=" + safeSource);
        }
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    binding, binding.getX(), binding.getY(),
                    largeRect[0], largeRect[1], largeRect[2], largeRect[3]);
        } catch (RuntimeException e) {
            log.warn("story objective no-detect capture mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
            return terminal(Status.MECHANICS_FAILED, "capture-mechanics-failed source=" + safeSource);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return terminal(Status.CAPTURE_UNAVAILABLE, "capture-unavailable source=" + safeSource);
        }
        BufferedImage image = captured.get().image();
        try {
            BufferedImage cropped = ImagePreprocessor.cropAbsoluteRect(image, largeRect, smallRect);
            if (cropped == null) {
                return terminal(Status.CROP_FAILED, "no-detect-crop-failed source=" + safeSource);
            }
            return capturedFrom(cropped, smallRect[0], smallRect[1], safeSource, false, true);
        } finally {
            image.flush();
        }
    }

    /**
     * {@code CROP_STORY_OBJECTIVE_FROM_WINDOW_SNAPSHOT}: crop the small story rect out of a caller-owned
     * full window snapshot using its screen-absolute origin, with zero new capture, mirroring
     * {@code cropStoryObjectiveFromWindowSnapshotNoDetect}.
     */
    public StoryObjectiveResult cropStoryObjectiveFromWindowSnapshot(WindowSnapshotCropCommand command) {
        Objects.requireNonNull(command, "command");
        String safeSource = safeSource(command.source());
        BufferedImage snapshot;
        try {
            snapshot = ImageIO.read(new ByteArrayInputStream(command.windowSnapshotPngBytes()));
        } catch (IOException e) {
            log.warn("story objective snapshot decode failed: source={} reason={}", safeSource, e.getMessage(), e);
            return terminal(Status.MECHANICS_FAILED, "snapshot-decode-failed source=" + safeSource);
        }
        if (snapshot == null) {
            return terminal(Status.MECHANICS_FAILED, "snapshot-null source=" + safeSource);
        }
        try {
            // Baseline window-origin conversion: screen-absolute small rect minus the snapshot origin
            // yields the window-local crop box; the absolute top-left is reported in the result.
            int absoluteLeft;
            int absoluteTop;
            int localLeft;
            int localTop;
            try {
                absoluteLeft = Math.addExact(command.windowBaseX(), DIALOG_SMALL_X);
                absoluteTop = Math.addExact(command.windowBaseY(), DIALOG_SMALL_Y);
                localLeft = absoluteLeft - command.windowBaseX();
                localTop = absoluteTop - command.windowBaseY();
            } catch (ArithmeticException e) {
                return terminal(Status.MECHANICS_FAILED, "snapshot-rect-overflow source=" + safeSource);
            }
            BufferedImage cropped = ImagePreprocessor.cropCopy(
                    snapshot, localLeft, localTop, DIALOG_SMALL_W, DIALOG_SMALL_H);
            if (cropped == null) {
                log.warn("story objective snapshot crop failed: source={} local=({}, {}) {}x{} snapshot={}x{}",
                        safeSource, localLeft, localTop, DIALOG_SMALL_W, DIALOG_SMALL_H,
                        snapshot.getWidth(), snapshot.getHeight());
                return terminal(Status.CROP_FAILED, "snapshot-crop-failed source=" + safeSource);
            }
            return capturedFrom(cropped, absoluteLeft, absoluteTop, safeSource, false, true);
        } finally {
            snapshot.flush();
        }
    }

    /**
     * Encode an owned objective image to a typed CAPTURED result: recompute SHA-256 and dimensions from
     * the real PNG bytes, save the latest+history debug images, and flush the image once when this method
     * owns its lifecycle. {@code fullFrameFallback} marks the op1 baseline full-frame fallback;
     * {@code flushImage=false} is used when the caller's own {@code finally} already flushes the image
     * (the shared detection frame reused by the fallback).
     */
    private StoryObjectiveResult capturedFrom(BufferedImage image, int absoluteLeft, int absoluteTop,
                                              String safeSource, boolean fullFrameFallback, boolean flushImage) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                return terminal(Status.CROP_FAILED, "image-empty source=" + safeSource);
            }
            byte[] pngBytes;
            try {
                pngBytes = pngBytes(image);
            } catch (IOException e) {
                log.warn("story objective encode failed: source={} reason={}", safeSource, e.getMessage(), e);
                return terminal(Status.MECHANICS_FAILED, "encode-failed source=" + safeSource);
            }
            String sha256;
            try {
                sha256 = sha256Hex(pngBytes);
            } catch (NoSuchAlgorithmException e) {
                return terminal(Status.MECHANICS_FAILED, "digest-unavailable source=" + safeSource);
            }
            saveStoryObjectiveDebugImage(safeSource, image);
            return new StoryObjectiveResult(Status.CAPTURED, fullFrameFallback, pngBytes, sha256, width, height,
                    absoluteLeft, absoluteTop,
                    (fullFrameFallback ? "captured-full-frame-fallback source=" : "captured source=") + safeSource);
        } finally {
            if (flushImage) {
                image.flush();
            }
        }
    }

    /** Latest + history debug save, mirroring the baseline story-objective debug rule. */
    private void saveStoryObjectiveDebugImage(String safeSource, BufferedImage image) {
        String latestPath = windowScopedTempPath.resolve("story_objective_" + safeSource + ".png");
        String historyPath = windowScopedTempPath.resolve(
                "story_objective_" + safeSource + "_" + System.currentTimeMillis() + ".png");
        saveStoryObjectiveDebugImageToPath(safeSource, image, latestPath);
        saveStoryObjectiveDebugImageToPath(safeSource, image, historyPath);
    }

    private void saveStoryObjectiveDebugImageToPath(String safeSource, BufferedImage image, String path) {
        if (ImagePreprocessor.saveImage(image, path)) {
            log.info("dialog story objective debug saved: source={} path={}", safeSource, path);
        } else {
            log.warn("dialog story objective debug save failed: source={} path={}", safeSource, path);
        }
    }

    private static int[] largeScreenRect(WindowNativeBinding binding) {
        return screenRect(binding, DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    private static int[] smallScreenRect(WindowNativeBinding binding) {
        return screenRect(binding, DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

    private static int[] screenRect(WindowNativeBinding binding, int offsetX, int offsetY, int width, int height) {
        try {
            int left = Math.addExact(binding.getX(), offsetX);
            int top = Math.addExact(binding.getY(), offsetY);
            int right = Math.addExact(left, width);
            int bottom = Math.addExact(top, height);
            return new int[]{left, top, right, bottom};
        } catch (ArithmeticException e) {
            return null;
        }
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(bytes);
        StringBuilder result = new StringBuilder(hashed.length * 2);
        for (byte value : hashed) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        String value = source.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private static StoryObjectiveResult terminal(Status status, String reason) {
        return new StoryObjectiveResult(status, false, null, null, null, null, null, null, reason);
    }
}
