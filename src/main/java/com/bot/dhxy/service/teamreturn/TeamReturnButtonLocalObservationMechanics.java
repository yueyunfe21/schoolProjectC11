package com.bot.dhxy.service.teamreturn;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Screenshot-only observation of the member-side return-team button inside one exact native-window
 * binding. It mirrors the committed {@code 0114604e} mechanics (configured team area,
 * {@code images/template/status/gui.png}, {@code returnTeamMatchRate}) but sources the frame from
 * {@link BoundWindowCaptureService} against the caller-supplied binding instead of the global first
 * window. It never sends input, never clicks, and does not go through
 * {@code CoordinateHelper.findImageInRegion}, which collapses capture failure and template miss into
 * one null.
 */
@Slf4j
@Service
public final class TeamReturnButtonLocalObservationMechanics {

    private static final String MEMBER_RETURN_BUTTON_PATH = "images/template/status/gui.png";

    private final BoundWindowCaptureService captureService;
    private final BotProperties botProperties;

    public TeamReturnButtonLocalObservationMechanics(BoundWindowCaptureService captureService,
                                                     BotProperties botProperties) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.botProperties = Objects.requireNonNull(botProperties, "botProperties");
    }

    /**
     * Observes the configured return-team button region for one exact native-window binding.
     *
     * @param binding exact binding accepted by the caller's current registration/fence gate; its geometry is
     *                screen-absolute pixels and must be non-null with a native handle and positive dimensions
     * @return a non-null closed result; only {@link State#PRESENT} carries window-client coordinates and a
     *         finite match score, while every other state carries null coordinates and score
     */
    public ObservationResult observe(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null);
        }

        int areaX = botProperties.getReturnTeamAreaX();
        int areaY = botProperties.getReturnTeamAreaY();
        int areaW = botProperties.getReturnTeamAreaW();
        int areaH = botProperties.getReturnTeamAreaH();

        int baseX = binding.getX();
        int baseY = binding.getY();
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    binding,
                    baseX,
                    baseY,
                    baseX + areaX,
                    baseY + areaY,
                    baseX + areaX + areaW,
                    baseY + areaY + areaH);
        } catch (RuntimeException e) {
            log.warn("[team-return] local observation capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null);
        }

        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null);
        }

        BufferedImage frame = captured.get().image();
        BufferedImage template = null;
        try {
            try {
                template = ImageIO.read(Path.of(MEMBER_RETURN_BUTTON_PATH).toFile());
            } catch (IOException e) {
                log.warn("[team-return] local observation template unreadable: path={} reason={}",
                        MEMBER_RETURN_BUTTON_PATH, e.getMessage(), e);
                return new ObservationResult(State.TEMPLATE_UNAVAILABLE, null, null, null);
            }
            if (template == null) {
                return new ObservationResult(State.TEMPLATE_UNAVAILABLE, null, null, null);
            }

            double threshold = botProperties.getReturnTeamMatchRate();
            double[] match = ImageFinder.find(frame, template, threshold);
            if (match == null) {
                return new ObservationResult(State.ABSENT, null, null, null);
            }
            if (!isValidMatch(match, areaW, areaH, threshold)) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null);
            }

            int roundedX = (int) Math.round(match[0]);
            int roundedY = (int) Math.round(match[1]);
            if (roundedX < 0 || roundedX >= areaW || roundedY < 0 || roundedY >= areaH) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null);
            }
            return new ObservationResult(
                    State.PRESENT,
                    areaX + roundedX,
                    areaY + roundedY,
                    match[2]);
        } catch (RuntimeException e) {
            log.warn("[team-return] local observation mechanics failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(State.MECHANICS_FAILED, null, null, null);
        } finally {
            frame.flush();
            if (template != null) {
                template.flush();
            }
        }
    }

    private boolean isValidMatch(double[] match, int areaW, int areaH, double threshold) {
        return match.length >= 3
                && Double.isFinite(match[0])
                && Double.isFinite(match[1])
                && Double.isFinite(match[2])
                && match[0] >= 0.0D
                && match[0] < areaW
                && match[1] >= 0.0D
                && match[1] < areaH
                && match[2] >= threshold;
    }

    public enum State {
        PRESENT,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    public record ObservationResult(
            State state,
            Integer clientX,
            Integer clientY,
            Double matchScore) {

        public ObservationResult {
            Objects.requireNonNull(state, "state");
            boolean present = state == State.PRESENT;
            boolean hasAllMatchFields = clientX != null && clientY != null && matchScore != null;
            if (present != hasAllMatchFields) {
                throw new IllegalArgumentException("match fields must be all present only for PRESENT");
            }
            if (!present && (clientX != null || clientY != null || matchScore != null)) {
                throw new IllegalArgumentException("non-PRESENT result must not carry match fields");
            }
            if (present && !Double.isFinite(matchScore)) {
                throw new IllegalArgumentException("PRESENT requires a finite matchScore");
            }
        }
    }
}
