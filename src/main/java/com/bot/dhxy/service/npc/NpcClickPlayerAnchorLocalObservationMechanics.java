package com.bot.dhxy.service.npc;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * W-696-NPC-PLAYER-ANCHOR-WHOLE-LOCAL-OBSERVATION-1: closed pure-image local observation for the
 * NPC first-shot purple player-name anchor pass.
 *
 * <p>Byte-behaviour authority is the Git-readable {@code 696a12b0
 * service/NpcClickService.java}: the {@code captureCleanNameRegionToMemory(prepareAlt4)} Alt+4 direct
 * segment (lines 3289-3316), the {@code prepareNpcOcrScanImage} default-mask/skip source preparation
 * (lines 2505-2531 -> {@code OcrWindowScanService.copyWithDefaultMasks}), the OpenCV purple wash
 * {@code ImagePreprocessor.washPurpleTextToBlackAndWhite} (HSV {@code inRange(120,50,50)-(160,255,255)}
 * then {@code bitwise_not}) and the {@code extractPurpleBlobAnchor} connected-purple-blob bounding box
 * (lines 3132-3189). The whole wash + blob is cohered here as private members exactly as the baseline
 * runs it (NOT the frozen shared {@code ImagePreprocessor}); the temp-file write/imread/imwrite/read hops
 * are reproduced in-memory over the identical PNG bytes ({@code ImageIO.write} == {@code encodePng},
 * {@code Imgcodecs.imread} == {@code Imgcodecs.imdecode}, {@code Imgcodecs.imwrite} ==
 * {@code Imgcodecs.imencode}), so every marshalling hop is byte-identical to the baseline file flow.</p>
 *
 * <p>This class produces zero player-name OCR, zero provider fallback, zero map formula, zero target
 * verdict, zero click/verify and zero retry: the Cloud {@code NpcClickService} keeps the player identity
 * OCR/strict match, the {@code UX/VX/UY/VY} map formula, the {@code -50} first-shot offset, candidate
 * verdict and click/verify. It owns no owner/session/ledger/TTL. When {@code prepareAlt4} is requested the
 * caller must already hold the exclusive input-worker segment; the baseline Alt+4 keypress and settle wait
 * run direct on that thread with no queue-in-queue submission. The borrowed binding is never owned; every
 * owned raw/source/washed/decoded image is flushed exactly once on the success, empty and exception
 * paths, and every OpenCV {@code Mat} is released.</p>
 */
@Slf4j
@Service
public final class NpcClickPlayerAnchorLocalObservationMechanics {

    // Baseline OcrWindowScanService default full-window source region and HUD/chat/shortcut masks.
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int[][] DEFAULT_MASKS = {
            {0, 0, 258, 200},
            {0, 0, 1024, 54},
            {768, 58, 1020, 160},
            {4, 735, 706, 768},
            {710, 700, 1024, 768}
    };

    // Baseline NpcClickService purple-blob gates, value-for-value (lines 127-132).
    private static final int PURPLE_BLOB_MIN_PIXELS = 20;
    private static final int PURPLE_BLOB_MIN_WIDTH = 8;
    private static final int PURPLE_BLOB_MIN_HEIGHT = 4;
    private static final int PURPLE_BLOB_MAX_PIXELS = 6000;
    private static final int PURPLE_BLOB_MAX_WIDTH = 360;
    private static final int PURPLE_BLOB_MAX_HEIGHT = 140;
    /** Baseline extractPurpleBlobAnchor dark-pixel threshold: {@code rgb & 0xFFFFFF < 0x303030}. */
    private static final int PURPLE_DARK_RGB_THRESHOLD = 0x303030;

    // Baseline ImagePreprocessor.washPurpleTextToBlackAndWhite HSV inRange bounds (OpenCV HSV space).
    private static final Scalar PURPLE_WASH_LOWER = new Scalar(120, 50, 50);
    private static final Scalar PURPLE_WASH_UPPER = new Scalar(160, 255, 255);

    // Baseline NpcClickService Alt+4 settle wait (NPC_PIPELINE_HIDE_PLAYER_NAMES_SETTLE_MS, line 135).
    private static final int HIDE_PLAYER_NAMES_SETTLE_MS = 400;
    private static final String INPUT_WORKER_THREAD_NAME_TOKEN = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final InputProvider inputProvider;

    public NpcClickPlayerAnchorLocalObservationMechanics(
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService,
            InputProvider inputProvider) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
    }

    /**
     * Observe the purple player-name anchor blob in one exact window-relative scan region.
     *
     * @param binding exact borrowed native-window binding; never owned or mutated here
     * @param command the closed caller-supplied window-relative scan region plus the caller-decided
     *                {@code prepareAlt4} / {@code skipDefaultMask} flags
     * @return closed typed result; only {@code CAPTURED}/{@code NO_PURPLE_BLOB} carry raw/mask evidence and
     *         the scan rect, and only {@code CAPTURED} carries a screen-absolute blob anchor
     */
    public Result observe(WindowNativeBinding binding, ScanRegion command) {
        Objects.requireNonNull(command, "command");
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }
        if (isInterrupted()) {
            return Result.state(Terminal.INTERRUPTED);
        }

        // Baseline captureCleanNameRegionToMemory(prepareAlt4=true) hides player names before the single
        // capture. The keypress + settle must run direct inside the caller's already-exclusive input-worker
        // segment (no queue-in-queue), so require that thread when Alt+4 is asked; a non-input-worker caller
        // cannot satisfy the mechanics contract and closes as MECHANICS_FAILED (the closed terminal set
        // carries no dedicated non-input-worker state). The baseline guards the settle with
        // {@code if (!TaskSleep.sleep(...)) return false}, so a false settle closes as INTERRUPTED below.
        if (command.prepareAlt4()) {
            if (!isInputWorkerThread()) {
                log.warn("npc player anchor Alt+4 requested off the input-worker thread: hwnd={} thread={}",
                        binding.getNativeHandle(), Thread.currentThread().getName());
                return Result.state(Terminal.MECHANICS_FAILED);
            }
            try {
                inputProvider.pressAlt4();
            } catch (RuntimeException e) {
                log.warn("npc player anchor player-name hide input failed: hwnd={} reason={}",
                        binding.getNativeHandle(), e.getMessage(), e);
                return Result.state(Terminal.MECHANICS_FAILED);
            }
            // Baseline captureCleanNameRegionToMemory guards this exact settle with
            // {@code if (!TaskSleep.sleep(...)) return false}: a false settle is a real pre-capture
            // interruption, so close as INTERRUPTED and never reach the single capture.
            if (!TaskSleep.sleep(HIDE_PLAYER_NAMES_SETTLE_MS)) {
                return Result.state(Terminal.INTERRUPTED);
            }
        }

        Optional<WindowNativeBinding> refreshed;
        try {
            refreshed = bindingRefreshService.refreshGeometry(binding);
        } catch (RuntimeException e) {
            log.warn("npc player anchor geometry refresh failed: hwnd={} reason={}",
                    binding.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        }
        if (refreshed.isEmpty()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }
        WindowNativeBinding fresh = refreshed.get();
        if (!fresh.hasNativeHandle() || !fresh.hasGeometry()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }

        int baseX = fresh.getX();
        int baseY = fresh.getY();
        int screenLeft;
        int screenTop;
        int screenRight;
        int screenBottom;
        try {
            screenLeft = Math.addExact(baseX, command.left());
            screenTop = Math.addExact(baseY, command.top());
            screenRight = Math.addExact(baseX, command.right());
            screenBottom = Math.addExact(baseY, command.bottom());
        } catch (ArithmeticException e) {
            return Result.state(Terminal.MECHANICS_FAILED);
        }

        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(fresh, baseX, baseY, screenLeft, screenTop, screenRight, screenBottom);
        } catch (RuntimeException e) {
            log.warn("npc player anchor capture failed: hwnd={} reason={}", fresh.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return Result.state(Terminal.CAPTURE_UNAVAILABLE);
        }

        BufferedImage raw = captured.get().image();
        BufferedImage source = null;
        boolean sourceIsSeparateCopy = false;
        BufferedImage washedImage = null;
        try {
            // Baseline calculatePlayerAnchorFormulaPoint gates {@code shouldStop()} right after the capture
            // and before prepareNpcOcrScanImage (696:2919). A hit here flushes the captured frame via the
            // outer finally and closes as INTERRUPTED, before any copy/mask/wash allocation.
            if (isInterrupted()) {
                return Result.state(Terminal.INTERRUPTED);
            }
            // Prepare the OCR source once: the default full-window fallback hides HUD/chat/shortcut unless
            // the caller skips the mask; every other region washes the raw crop directly.
            if (isDefaultMaskedWindowRegion(command) && !command.skipDefaultMask()) {
                source = copyWithDefaultMasks(raw);
                if (source == null) {
                    return Result.state(Terminal.MECHANICS_FAILED);
                }
                sourceIsSeparateCopy = true;
            } else {
                source = raw;
            }

            // Baseline gates {@code shouldStop()} again after the source PNG is written and before
            // washPurpleTextToBlackAndWhite (696:2941). A hit here flushes source + raw via the outer
            // finally and closes as INTERRUPTED, before the OpenCV wash allocation.
            byte[] sourcePng = encodePng(source);
            if (isInterrupted()) {
                return Result.state(Terminal.INTERRUPTED);
            }
            // Baseline wash flow over identical PNG bytes: encode source -> OpenCV imdecode -> BGR2HSV ->
            // purple inRange -> bitwise_not -> imencode. A failed decode/encode is a closed MECHANICS_FAILED.
            byte[] washedPng = washPurpleToBlackOnWhitePng(sourcePng);
            if (washedPng == null) {
                return Result.state(Terminal.MECHANICS_FAILED);
            }
            washedImage = decodePng(washedPng);
            if (washedImage == null) {
                return Result.state(Terminal.MECHANICS_FAILED);
            }

            PurpleBlob blob = detectPurpleBlob(washedImage, screenLeft, screenTop);

            ImageEvidence rawEvidence = ImageEvidence.of(source);
            ImageEvidence maskEvidence = ImageEvidence.of(washedImage);
            int[] scanRect = new int[]{screenLeft, screenTop, screenRight, screenBottom};

            if (blob == null) {
                return Result.captured(Terminal.NO_PURPLE_BLOB, null, rawEvidence, maskEvidence, scanRect);
            }
            return Result.captured(Terminal.CAPTURED, blob, rawEvidence, maskEvidence, scanRect);
        } catch (RuntimeException e) {
            log.warn("npc player anchor mechanics failed: hwnd={} reason={}", fresh.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        } finally {
            if (washedImage != null) {
                washedImage.flush();
            }
            if (sourceIsSeparateCopy && source != null && source != raw) {
                source.flush();
            }
            raw.flush();
        }
    }

    private static boolean isDefaultMaskedWindowRegion(ScanRegion region) {
        return region.left() == 0 && region.top() == 0
                && region.right() == WINDOW_WIDTH && region.bottom() == WINDOW_HEIGHT;
    }

    /** Ported baseline OcrWindowScanService copy + default HUD/chat/shortcut masks. */
    private static BufferedImage copyWithDefaultMasks(BufferedImage source) {
        if (source == null) {
            return null;
        }
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        boolean handedOff = false;
        try {
            Graphics2D drawGraphics = copy.createGraphics();
            try {
                drawGraphics.drawImage(source, 0, 0, null);
            } finally {
                drawGraphics.dispose();
            }
            Graphics2D maskGraphics = copy.createGraphics();
            try {
                maskGraphics.setColor(Color.WHITE);
                for (int[] mask : DEFAULT_MASKS) {
                    int left = clampValue(Math.min(mask[0], mask[2]), 0, Math.max(0, copy.getWidth()));
                    int right = clampValue(Math.max(mask[0], mask[2]), 0, Math.max(0, copy.getWidth()));
                    int top = clampValue(Math.min(mask[1], mask[3]), 0, Math.max(0, copy.getHeight()));
                    int bottom = clampValue(Math.max(mask[1], mask[3]), 0, Math.max(0, copy.getHeight()));
                    if (right > left && bottom > top) {
                        maskGraphics.fillRect(left, top, right - left, bottom - top);
                    }
                }
            } finally {
                maskGraphics.dispose();
            }
            handedOff = true;
            return copy;
        } finally {
            // On exceptional exit the local copy never reaches the outer owner, so flush it here
            // exactly once; on success ownership transfers to the caller's finally (no double flush).
            if (!handedOff) {
                copy.flush();
            }
        }
    }

    private static int clampValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Ported baseline ImagePreprocessor.washPurpleTextToBlackAndWhite, in-memory over the identical PNG
     * bytes: OpenCV imdecode(COLOR) -> BGR2HSV -> purple inRange -> bitwise_not -> imencode(".png"). The
     * washed output is the inverted mask: purple text pixels become black (below the dark threshold), the
     * background becomes white. Every Mat is released. Returns null on any decode/encode miss.
     */
    private static byte[] washPurpleToBlackOnWhitePng(byte[] sourcePng) {
        // Every native owner starts null and is acquired inside the try, so a RuntimeException from any
        // constructor or OpenCV call still reaches the finally, which releases each nonnull owner exactly
        // once. Acquisition order (imdecode -> BGR2HSV -> inRange -> bitwise_not -> imencode) is unchanged.
        MatOfByte srcBuf = null;
        Mat src = null;
        Mat hsv = null;
        Mat mask = null;
        Mat inverted = null;
        MatOfByte encoded = null;
        try {
            srcBuf = new MatOfByte(sourcePng);
            src = Imgcodecs.imdecode(srcBuf, Imgcodecs.IMREAD_COLOR);
            if (src == null || src.empty()) {
                return null;
            }
            hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);
            mask = new Mat();
            Core.inRange(hsv, PURPLE_WASH_LOWER, PURPLE_WASH_UPPER, mask);
            inverted = new Mat();
            Core.bitwise_not(mask, inverted);
            encoded = new MatOfByte();
            if (!Imgcodecs.imencode(".png", inverted, encoded)) {
                return null;
            }
            return encoded.toArray();
        } finally {
            if (srcBuf != null) {
                srcBuf.release();
            }
            if (src != null) {
                src.release();
            }
            if (hsv != null) {
                hsv.release();
            }
            if (mask != null) {
                mask.release();
            }
            if (inverted != null) {
                inverted.release();
            }
            if (encoded != null) {
                encoded.release();
            }
        }
    }

    /**
     * Ported baseline extractPurpleBlobAnchor: the connected-purple-blob is the single bounding box over
     * every dark ({@code rgb & 0xFFFFFF < 0x303030}) pixel of the washed image. A blob that is too small,
     * too wide/tall or too dense is rejected (the purple washer caught UI/chat/background noise), returning
     * a null anchor. The surviving blob is mapped to screen-absolute coordinates from the scan-region origin.
     */
    private static PurpleBlob detectPurpleBlob(BufferedImage washedImage, int scanStartX, int scanStartY) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int darkPixels = 0;
        for (int y = 0; y < washedImage.getHeight(); y++) {
            for (int x = 0; x < washedImage.getWidth(); x++) {
                int rgb = washedImage.getRGB(x, y) & 0xFFFFFF;
                if (rgb < PURPLE_DARK_RGB_THRESHOLD) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    darkPixels++;
                }
            }
        }
        if (darkPixels < PURPLE_BLOB_MIN_PIXELS || minX == Integer.MAX_VALUE) {
            return null;
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        if (width < PURPLE_BLOB_MIN_WIDTH || height < PURPLE_BLOB_MIN_HEIGHT
                || width > PURPLE_BLOB_MAX_WIDTH || height > PURPLE_BLOB_MAX_HEIGHT
                || darkPixels > PURPLE_BLOB_MAX_PIXELS) {
            return null;
        }
        int rectLeft = Math.addExact(scanStartX, minX);
        int rectTop = Math.addExact(scanStartY, minY);
        int rectRight = Math.addExact(scanStartX, maxX);
        int rectBottom = Math.addExact(scanStartY, maxY);
        int anchorX = Math.addExact(scanStartX, (minX + maxX) / 2);
        int anchorY = Math.addExact(scanStartY, (minY + maxY) / 2);
        return new PurpleBlob(rectLeft, rectTop, rectRight, rectBottom, anchorX, anchorY, darkPixels);
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_NAME_TOKEN);
    }

    private static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /** 8-byte PNG signature (89 50 4E 47 0D 0A 1A 0A). */
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static boolean hasPngMagic(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage decodePng(byte[] pngBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("no PNG writer available");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("failed to encode PNG evidence", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ===================== closed immutable nested types =====================

    /** Six closed terminals; only CAPTURED/NO_PURPLE_BLOB carry evidence + rect. */
    public enum Terminal {
        CAPTURED,
        NO_PURPLE_BLOB,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * Closed caller-supplied window-relative scan region (positive-area box) plus the caller-decided
     * {@code prepareAlt4} and {@code skipDefaultMask} flags. When the region is the default full-window
     * fallback and {@code skipDefaultMask} is false, the baseline HUD/chat/shortcut masks are applied once
     * before the purple wash. When {@code prepareAlt4} is true the baseline Alt+4 player-name hide runs once
     * before the single capture.
     */
    public record ScanRegion(int left, int top, int right, int bottom, boolean prepareAlt4, boolean skipDefaultMask) {
        public ScanRegion {
            if (right <= left || bottom <= top) {
                throw new IllegalArgumentException("scan region must be a positive-area window-relative box");
            }
        }
    }

    /** One closed screen-absolute purple player-name blob; pure geometry, no OCR text, no verdict. */
    public record PurpleBlob(
            int rectLeft, int rectTop, int rectRight, int rectBottom,
            int anchorX, int anchorY,
            int darkPixels) {
        public PurpleBlob {
            if (rectRight < rectLeft || rectBottom < rectTop) {
                throw new IllegalArgumentException("purple blob rect must be a non-negative-area box");
            }
            if (anchorX < rectLeft || anchorX > rectRight || anchorY < rectTop || anchorY > rectBottom) {
                throw new IllegalArgumentException("purple blob anchor must lie inside the blob rect");
            }
            if (darkPixels < PURPLE_BLOB_MIN_PIXELS || darkPixels > PURPLE_BLOB_MAX_PIXELS) {
                throw new IllegalArgumentException("purple blob dark-pixel count is outside the baseline bounds");
            }
        }
    }

    /** Raw or mask PNG evidence, structurally validated from the actual bytes. */
    public record ImageEvidence(byte[] pngBytes, String sha256, int width, int height) {
        public ImageEvidence {
            Objects.requireNonNull(pngBytes, "pngBytes");
            Objects.requireNonNull(sha256, "sha256");
            pngBytes = pngBytes.clone();
            if (pngBytes.length == 0) {
                throw new IllegalArgumentException("image evidence requires PNG bytes");
            }
            if (!hasPngMagic(pngBytes)) {
                throw new IllegalArgumentException("image evidence bytes do not carry the PNG signature");
            }
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(pngBytes));
            } catch (IOException e) {
                throw new IllegalArgumentException("image evidence bytes are not decodable PNG", e);
            }
            if (decoded == null) {
                throw new IllegalArgumentException("image evidence bytes are not a PNG image");
            }
            try {
                if (decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw new IllegalArgumentException("image evidence dimensions do not match the PNG bytes");
                }
                if (!sha256Hex(pngBytes).equalsIgnoreCase(sha256)) {
                    throw new IllegalArgumentException("image evidence SHA-256 does not match the PNG bytes");
                }
            } finally {
                decoded.flush();
            }
        }

        private static ImageEvidence of(BufferedImage image) {
            byte[] bytes = encodePng(image);
            return new ImageEvidence(bytes, sha256Hex(bytes), image.getWidth(), image.getHeight());
        }

        @Override
        public byte[] pngBytes() {
            return pngBytes.clone();
        }
    }

    /**
     * Closed result. {@code CAPTURED} carries the screen-absolute purple blob plus raw/mask evidence and the
     * screen-absolute scan rect; {@code NO_PURPLE_BLOB} carries the same evidence and rect with a null blob;
     * every other terminal carries none of them.
     */
    public record Result(
            Terminal terminal,
            PurpleBlob blob,
            ImageEvidence rawEvidence,
            ImageEvidence maskEvidence,
            int[] scanRect) {

        public Result {
            Objects.requireNonNull(terminal, "terminal");
            scanRect = scanRect == null ? null : scanRect.clone();
            boolean carriesEvidence = terminal == Terminal.CAPTURED || terminal == Terminal.NO_PURPLE_BLOB;
            if (carriesEvidence) {
                Objects.requireNonNull(rawEvidence, "rawEvidence");
                Objects.requireNonNull(maskEvidence, "maskEvidence");
                if (scanRect == null || scanRect.length != 4) {
                    throw new IllegalArgumentException("evidence-carrying result requires a 4-element scan rect");
                }
                int spanWidth = scanRect[2] - scanRect[0];
                int spanHeight = scanRect[3] - scanRect[1];
                if (spanWidth <= 0 || spanHeight <= 0) {
                    throw new IllegalArgumentException("scan rect must be a positive-area span");
                }
                if (rawEvidence.width() != maskEvidence.width() || rawEvidence.height() != maskEvidence.height()) {
                    throw new IllegalArgumentException("raw and mask evidence dimensions must be identical");
                }
                if (rawEvidence.width() != spanWidth || rawEvidence.height() != spanHeight) {
                    throw new IllegalArgumentException("evidence dimensions must equal the scan rect span");
                }
                if (terminal == Terminal.CAPTURED) {
                    if (blob == null) {
                        throw new IllegalArgumentException("CAPTURED must carry a purple blob");
                    }
                    // scanRect right/bottom are exclusive spans; the blob rect is inclusive, so its right/bottom
                    // must stay strictly below the exclusive upper bound.
                    if (blob.rectLeft() < scanRect[0] || blob.rectTop() < scanRect[1]
                            || blob.rectRight() >= scanRect[2] || blob.rectBottom() >= scanRect[3]) {
                        throw new IllegalArgumentException("CAPTURED blob rect must lie inside the scan rect");
                    }
                }
                if (terminal == Terminal.NO_PURPLE_BLOB && blob != null) {
                    throw new IllegalArgumentException("NO_PURPLE_BLOB must not carry a blob");
                }
            } else if (blob != null || rawEvidence != null || maskEvidence != null || scanRect != null) {
                throw new IllegalArgumentException("non-evidence terminal must not carry a blob, evidence, or rect");
            }
        }

        private static Result state(Terminal terminal) {
            return new Result(terminal, null, null, null, null);
        }

        private static Result captured(Terminal terminal, PurpleBlob blob,
                                       ImageEvidence rawEvidence, ImageEvidence maskEvidence, int[] scanRect) {
            return new Result(terminal, blob, rawEvidence, maskEvidence, scanRect);
        }

        @Override
        public int[] scanRect() {
            return scanRect == null ? null : scanRect.clone();
        }
    }
}
