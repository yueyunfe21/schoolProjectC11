package com.bot.dhxy.service.dialog;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Optional;

/**
 * Closed local mechanics for the {@code DIALOG_PREPARED_ACTION_VALIDATION} macro. It reproduces the
 * committed capture/wash/fingerprint/distance body of {@code 696a12b0 DialogService
 * .validatePreparedDialogActionForConsume:1185-1193} exactly: outside any input queue it reads the exact
 * HWND fresh geometry, takes a single screen-absolute validation crop, washes it by the caller's mode,
 * builds the binary fingerprint, and compares it to the caller's expected fingerprint against the
 * caller-supplied max distance. It sends no input and never selects an action/target/fallback/timestamp;
 * the null / clickRequired / fingerprint-present pre-capture gates and the {@code lastVerifiedAtMs}
 * refresh stay entirely on the Cloud side.
 *
 * <p>Closed terminal: only {@link State#VALIDATED} and {@link State#FINGERPRINT_MISMATCH} carry the
 * current fingerprint, distance and max distance; the other states carry none of them.</p>
 */
@Slf4j
@Service
public final class DialogPreparedActionValidationLocalMechanics {

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public DialogPreparedActionValidationLocalMechanics(BoundWindowCaptureService captureService,
                                                        WindowNativeBindingRefreshService bindingRefreshService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
    }

    /**
     * Validate one prepared dialog action's fingerprint against a fresh capture of its validation rect.
     *
     * @param binding            exact native-window binding; screen-absolute base with a handle and geometry
     * @param validationLeft     screen-absolute left of the validation rect
     * @param validationTop      screen-absolute top
     * @param validationRight    screen-absolute right
     * @param validationBottom   screen-absolute bottom
     * @param washMode           committed colour-clean mode used before fingerprinting
     * @param expectedFingerprint prepared action's stored binary fingerprint (non-blank; gated by Cloud)
     * @param maxDistance        committed operation max Hamming distance (8, or 16 for XIULUO_ENTER_BATTLE)
     * @return a non-null closed result; only VALIDATED/FINGERPRINT_MISMATCH carry fingerprint/distance
     */
    public PreparedActionValidationResult validate(WindowNativeBinding binding,
                                                   int validationLeft,
                                                   int validationTop,
                                                   int validationRight,
                                                   int validationBottom,
                                                   DialogFingerprintWashMode washMode,
                                                   String expectedFingerprint,
                                                   int maxDistance) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return PreparedActionValidationResult.terminal(State.BINDING_UNAVAILABLE);
        }
        // Baseline validation rect gate: an empty or inverted rect can never be captured.
        if (validationRight <= validationLeft || validationBottom <= validationTop) {
            return PreparedActionValidationResult.terminal(State.INVALID_RECT);
        }
        BufferedImage raw = null;
        BufferedImage washed = null;
        try {
            // Fresh exact-HWND geometry so the single window capture is cropped at the window's current
            // screen base and the screen-absolute rect maps to the same pixels the baseline would grab.
            // Inside the closed exception boundary: a refresh RuntimeException becomes MECHANICS_FAILED
            // (696 returns null for a validation exception), while an empty / handle-less / geometry-less
            // refresh is the closed BINDING_UNAVAILABLE. Still exactly one refresh and one capture.
            Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshGeometry(binding);
            if (refreshed.isEmpty() || !refreshed.get().hasNativeHandle() || !refreshed.get().hasGeometry()) {
                return PreparedActionValidationResult.terminal(State.BINDING_UNAVAILABLE);
            }
            WindowNativeBinding freshBinding = refreshed.get();
            Optional<BoundWindowCaptureService.CaptureResult> captured = captureService.captureRegion(
                    freshBinding, freshBinding.getX(), freshBinding.getY(),
                    validationLeft, validationTop, validationRight, validationBottom);
            if (captured.isEmpty() || captured.get().image() == null) {
                return PreparedActionValidationResult.terminal(State.CAPTURE_UNAVAILABLE);
            }
            raw = captured.get().image();
            washed = washPreparedValidationCrop(raw, washMode);
            if (washed == null) {
                return PreparedActionValidationResult.terminal(State.CAPTURE_UNAVAILABLE);
            }
            String currentFingerprint = ImagePreprocessor.buildBinaryFingerprint(washed);
            int distance = ImagePreprocessor.binaryFingerprintDistance(expectedFingerprint, currentFingerprint);
            State state = distance <= maxDistance ? State.VALIDATED : State.FINGERPRINT_MISMATCH;
            return PreparedActionValidationResult.measured(state, currentFingerprint, distance, maxDistance);
        } catch (RuntimeException e) {
            log.debug("[prepared-action-validate] mechanics failed: reason={}", e.getMessage(), e);
            return PreparedActionValidationResult.terminal(State.MECHANICS_FAILED);
        } finally {
            if (raw != null) {
                raw.flush();
            }
            if (washed != null && washed != raw) {
                washed.flush();
            }
        }
    }

    // Mirror of 696a12b0 DialogService.washPreparedValidationCrop:1231-1244, mode-for-mode.
    private BufferedImage washPreparedValidationCrop(BufferedImage raw, DialogFingerprintWashMode washMode) {
        if (raw == null) {
            return null;
        }
        if (washMode == DialogFingerprintWashMode.YELLOW) {
            return ImagePreprocessor.washYellowTextToBlackAndWhite(raw);
        }
        if (washMode == DialogFingerprintWashMode.GREEN) {
            return ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
        }
        if (washMode == DialogFingerprintWashMode.WHITE) {
            return ImagePreprocessor.washThinWhiteTextToBlackAndWhite(raw);
        }
        return ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(raw);
    }

    public enum State {
        VALIDATED,
        FINGERPRINT_MISMATCH,
        CAPTURE_UNAVAILABLE,
        INVALID_RECT,
        BINDING_UNAVAILABLE,
        MECHANICS_FAILED
    }

    /**
     * Closed validation result. {@code currentFingerprint/distance/maxDistance} are present only for
     * {@link State#VALIDATED} and {@link State#FINGERPRINT_MISMATCH}; every other state leaves them null.
     */
    public record PreparedActionValidationResult(State state,
                                                 String currentFingerprint,
                                                 Integer distance,
                                                 Integer maxDistance) {

        public PreparedActionValidationResult {
            Objects.requireNonNull(state, "state");
            boolean measured = state == State.VALIDATED || state == State.FINGERPRINT_MISMATCH;
            boolean hasMetrics = currentFingerprint != null && distance != null && maxDistance != null;
            boolean hasAnyMetric = currentFingerprint != null || distance != null || maxDistance != null;
            if (measured && !hasMetrics) {
                throw new IllegalArgumentException(state + " result requires fingerprint, distance and maxDistance");
            }
            if (!measured && hasAnyMetric) {
                throw new IllegalArgumentException(state + " result must not carry fingerprint/distance metrics");
            }
            if (measured) {
                if (currentFingerprint.isBlank()) {
                    throw new IllegalArgumentException("measured result requires a non-blank fingerprint");
                }
                if (distance < 0) {
                    throw new IllegalArgumentException("measured result requires a non-negative distance");
                }
                if (maxDistance != 8 && maxDistance != 16) {
                    throw new IllegalArgumentException("measured result maxDistance must be 8 or 16");
                }
                if ((state == State.VALIDATED) != (distance <= maxDistance)) {
                    throw new IllegalArgumentException(
                            "VALIDATED requires distance <= maxDistance; FINGERPRINT_MISMATCH requires distance > maxDistance");
                }
            }
        }

        private static PreparedActionValidationResult terminal(State state) {
            return new PreparedActionValidationResult(state, null, null, null);
        }

        private static PreparedActionValidationResult measured(State state, String currentFingerprint,
                                                               int distance, int maxDistance) {
            return new PreparedActionValidationResult(state, currentFingerprint, distance, maxDistance);
        }
    }
}
