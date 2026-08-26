package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.tools.CoordinateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Executes one Cloud-authorized Xinshou recovery mechanic against the currently bound window.
 *
 * <p>This collaborator owns only local mechanics: exact-window capture, allow-listed template
 * matching and one atomic input submission. It has no scheduler, consumed state, retry policy or
 * business event publication. A returned {@link Status#INPUT_APPLIED} means only that the requested
 * physical input completed; it is not proof that the Xinshou business phase succeeded.</p>
 */
@Component
public final class XinshouRecoveryLocalMechanics {

    private static final Logger log = LoggerFactory.getLogger(XinshouRecoveryLocalMechanics.class);
    private static final int CLICK_SETTLE_MS = 80;
    private static final int POST_INPUT_DELAY_MS = 150;

    private final RecoveryTargetResolver targetResolver;
    private final RecoveryInput recoveryInput;

    /**
     * @param tracker bound-window screenshot provider used for the current task window
     * @param coordinates converts the shared recovery ROI into screen-absolute pixels
     * @param inputSequences serialized physical-input boundary
     */
    @Autowired
    public XinshouRecoveryLocalMechanics(
            GameClientTracker tracker,
            CoordinateHelper coordinates,
            InputSequences inputSequences) {
        this(
                template -> resolveTarget(tracker, coordinates, template),
                new RecoveryInput() {
                    @Override
                    public boolean pressEscape() {
                        return inputSequences.submitAndWait(
                                "xinshou:cloud-recovery:escape",
                                List.of(
                                        InputAction.pressEscape(),
                                        InputAction.sleep(POST_INPUT_DELAY_MS)));
                    }

                    @Override
                    public boolean clickTemplate(String templateName, Point absolutePoint) {
                        return inputSequences.moveAndClickLeft(
                                "xinshou:cloud-recovery:" + templateName,
                                absolutePoint.x,
                                absolutePoint.y,
                                CLICK_SETTLE_MS,
                                POST_INPUT_DELAY_MS);
                    }
                });
    }

    XinshouRecoveryLocalMechanics(
            RecoveryTargetResolver targetResolver,
            RecoveryInput recoveryInput) {
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.recoveryInput = Objects.requireNonNull(recoveryInput, "recoveryInput");
    }

    /**
     * Applies exactly one ESC request through the current window's serialized input binding.
     *
     * @return mechanical input result; never a Xinshou business completion verdict
     */
    public Result pressEscapeOnce() {
        try {
            return recoveryInput.pressEscape()
                    ? new Result(Status.INPUT_APPLIED, null)
                    : new Result(Status.INPUT_FAILED, null);
        } catch (RuntimeException error) {
            log.warn("Xinshou recovery ESC input failed without retry: {}", error.getMessage(), error);
            return new Result(Status.INPUT_FAILED, null);
        }
    }

    /**
     * Re-captures the current window, matches one allow-listed recovery template and atomically
     * clicks its center at most once.
     *
     * @param templateName exact allow-listed template filename: {@code tiaoguo.png},
     *                     {@code quedingguan_.png}, or {@code confirm.png}; null and unknown values
     *                     are rejected
     * @return mechanical capture/match/input result; misses and failures are not retried
     */
    public Result matchAndClickOnce(String templateName) {
        XinshouAnchorLocalMechanics.RecoveryTemplateSpec template =
                XinshouAnchorLocalMechanics.recoveryTemplateSpec(templateName);
        if (template == null) {
            return new Result(Status.UNSUPPORTED_TEMPLATE, templateName);
        }

        TargetResolution resolution;
        try {
            resolution = targetResolver.resolve(template);
        } catch (RuntimeException error) {
            log.warn("Xinshou recovery template match failed without retry: template={} reason={}",
                    templateName, error.getMessage(), error);
            return new Result(Status.TEMPLATE_NOT_MATCHED, templateName);
        }
        if (resolution.status() != ResolutionStatus.MATCHED || resolution.absolutePoint() == null) {
            return new Result(resolution.status().publicStatus(), templateName);
        }

        try {
            boolean clicked = recoveryInput.clickTemplate(templateName, resolution.absolutePoint());
            return new Result(clicked ? Status.INPUT_APPLIED : Status.INPUT_FAILED, templateName);
        } catch (RuntimeException error) {
            log.warn("Xinshou recovery template input failed without retry: template={} reason={}",
                    templateName, error.getMessage(), error);
            return new Result(Status.INPUT_FAILED, templateName);
        }
    }

    private static TargetResolution resolveTarget(
            GameClientTracker tracker,
            CoordinateHelper coordinates,
            XinshouAnchorLocalMechanics.RecoveryTemplateSpec templateSpec) {
        BufferedImage template = null;
        BufferedImage currentWindowRoi = null;
        try {
            template = ImageIO.read(Path.of(templateSpec.path()).toFile());
            if (template == null) {
                return TargetResolution.of(ResolutionStatus.TEMPLATE_UNAVAILABLE);
            }

            int[] rect = coordinates.getScaledRect(
                    templateSpec.left(),
                    templateSpec.top(),
                    templateSpec.width(),
                    templateSpec.height());
            currentWindowRoi = tracker.captureToMemory(
                    "xinshou-recovery-" + templateSpec.templateName(),
                    rect[0],
                    rect[1],
                    rect[2],
                    rect[3]);
            if (currentWindowRoi == null) {
                return TargetResolution.of(ResolutionStatus.CAPTURE_UNAVAILABLE);
            }

            double[] match = ImageFinder.find(
                    currentWindowRoi,
                    template,
                    XinshouAnchorLocalMechanics.recoveryMatchThreshold());
            // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
            MatchEvidenceStore.save("xinshou-recovery-template", null, currentWindowRoi, template, match);
            Point absolutePoint = coordinates.resolveMatchedPointInRect(rect, match);
            return absolutePoint == null
                    ? TargetResolution.of(ResolutionStatus.TEMPLATE_NOT_MATCHED)
                    : TargetResolution.matched(absolutePoint);
        } catch (IOException error) {
            log.warn("Xinshou recovery template unavailable: template={} path={} reason={}",
                    templateSpec.templateName(), templateSpec.path(), error.getMessage());
            return TargetResolution.of(ResolutionStatus.TEMPLATE_UNAVAILABLE);
        } finally {
            if (currentWindowRoi != null) {
                currentWindowRoi.flush();
            }
            if (template != null) {
                template.flush();
            }
        }
    }

    public enum Status {
        INPUT_APPLIED,
        INPUT_FAILED,
        UNSUPPORTED_TEMPLATE,
        TEMPLATE_UNAVAILABLE,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_NOT_MATCHED
    }

    public record Result(Status status, String templateName) {
        public Result {
            Objects.requireNonNull(status, "status");
        }
    }

    enum ResolutionStatus {
        MATCHED(null),
        TEMPLATE_UNAVAILABLE(Status.TEMPLATE_UNAVAILABLE),
        CAPTURE_UNAVAILABLE(Status.CAPTURE_UNAVAILABLE),
        TEMPLATE_NOT_MATCHED(Status.TEMPLATE_NOT_MATCHED);

        private final Status publicStatus;

        ResolutionStatus(Status publicStatus) {
            this.publicStatus = publicStatus;
        }

        Status publicStatus() {
            if (publicStatus == null) {
                throw new IllegalStateException("matched target has no failure status");
            }
            return publicStatus;
        }
    }

    record TargetResolution(ResolutionStatus status, Point absolutePoint) {
        static TargetResolution of(ResolutionStatus status) {
            return new TargetResolution(status, null);
        }

        static TargetResolution matched(Point point) {
            return new TargetResolution(ResolutionStatus.MATCHED, point);
        }
    }

    @FunctionalInterface
    interface RecoveryTargetResolver {
        TargetResolution resolve(XinshouAnchorLocalMechanics.RecoveryTemplateSpec template);
    }

    interface RecoveryInput {
        boolean pressEscape();

        boolean clickTemplate(String templateName, Point absolutePoint);
    }
}
