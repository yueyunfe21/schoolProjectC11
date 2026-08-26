package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnMatchResult;
import com.bot.dhxy.cloud.turn.protocol.TurnMatchSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Executes only an explicitly requested local MATCH_TEMPLATE observation against one exact bound window.
 *
 * <p>This boundary performs no input. A successful {@code onMatch=CLICK} only marks the returned execution as
 * click-requested; TURN-11 must compose that coordinate with the existing input executor inside the same action.</p>
 */
public final class TurnMatchStepExecutor {

    private final TurnTemplateCache templateCache;
    private final TurnCaptureStepExecutor captureStepExecutor;

    public TurnMatchStepExecutor(TurnTemplateCache templateCache,
                                 TurnCaptureStepExecutor captureStepExecutor) {
        this.templateCache = Objects.requireNonNull(templateCache, "templateCache");
        this.captureStepExecutor = Objects.requireNonNull(captureStepExecutor, "captureStepExecutor");
    }

    /**
     * Capture and match one explicit protocol MATCH_TEMPLATE step without issuing input or choosing fallback policy.
     *
     * @param window exact-window snapshot whose rectangle uses unscaled screen-absolute pixels
     * @param step validated MATCH_TEMPLATE step; its region is screen-absolute or null for the full bound window
     * @return typed match, whether TURN-11 should compose a click, and the same capture frame only when requested
     * @throws TurnTransportException when the named template cannot be resolved to its exact Cloud-owned hash
     * @throws IllegalArgumentException when the supplied step is not an explicit valid MATCH_TEMPLATE step
     * @throws IllegalStateException when captured or cached PNG pixels cannot be decoded or matched safely
     */
    public Execution execute(TurnExecutionWindow window, TurnStep step) throws TurnTransportException {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(step, "step");
        if (step.type() != TurnStepType.MATCH_TEMPLATE || step.match() == null) {
            throw new IllegalArgumentException("TurnMatchStepExecutor requires an explicit MATCH_TEMPLATE step");
        }

        TurnMatchSpec matchSpec = step.match();
        requireMatchSpec(matchSpec);
        Path templatePath = templateCache.resolveTemplate(matchSpec.templateKey(), matchSpec.contentHash());
        TurnFrame captured = captureStepExecutor.capture(
                window,
                matchSpec.region(),
                TurnFramePurpose.MATCH_EVIDENCE,
                step.index());

        BufferedImage source = decodeCapturedPng(captured);
        BufferedImage template = decodeTemplatePng(templatePath);
        try {
            requireTemplateFitsCapture(template, source);
            double[] candidate = ImageFinder.find(source, template, matchSpec.threshold());
            // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
            MatchEvidenceStore.save("turn-match-step",
                    window.context() == null ? null : window.context().getWindowId(),
                    source, template, candidate);
            TurnMatchResult match = candidate == null
                    ? new TurnMatchResult(false, 0.0D, null, null, null)
                    : toAbsoluteMatch(candidate, template, captured.metadata().region());
            boolean clickRequested = match.found() && matchSpec.onMatch() == TurnMatchSpec.OnMatch.CLICK;
            TurnFrame returnedFrame = matchSpec.resultMode()
                    == TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT_AND_IMAGE ? captured : null;
            return new Execution(match, clickRequested, returnedFrame);
        } finally {
            source.flush();
            template.flush();
        }
    }

    private static void requireMatchSpec(TurnMatchSpec spec) {
        if (spec.templateKey() == null || spec.templateKey().isBlank()) {
            throw new IllegalArgumentException("match.templateKey must not be blank");
        }
        if (spec.contentHash() == null || !spec.contentHash().matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("match.contentHash must be a SHA-256 hexadecimal value");
        }
        if (!Double.isFinite(spec.threshold()) || spec.threshold() < 0.0D || spec.threshold() > 1.0D) {
            throw new IllegalArgumentException("match.threshold must be in [0.0, 1.0]");
        }
        if (spec.onMatch() == null || spec.resultMode() == null) {
            throw new IllegalArgumentException("match.onMatch and match.resultMode must not be null");
        }
    }

    private static BufferedImage decodeCapturedPng(TurnFrame frame) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(frame.pngBytes())) {
            BufferedImage image = ImageIO.read(input);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalStateException("MATCH_TEMPLATE capture is not a decodable PNG image");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("MATCH_TEMPLATE capture PNG cannot be decoded", e);
        }
    }

    private static BufferedImage decodeTemplatePng(Path templatePath) {
        try {
            BufferedImage image = ImageIO.read(templatePath.toFile());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalStateException("MATCH_TEMPLATE template is not a decodable PNG image");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("MATCH_TEMPLATE template PNG cannot be decoded", e);
        }
    }

    private static void requireTemplateFitsCapture(BufferedImage template, BufferedImage source) {
        if (template.getWidth() > source.getWidth() || template.getHeight() > source.getHeight()) {
            throw new IllegalStateException("MATCH_TEMPLATE template dimensions exceed the captured region");
        }
    }

    private static TurnMatchResult toAbsoluteMatch(double[] candidate,
                                                   BufferedImage template,
                                                   TurnRegion capturedRegion) {
        if (candidate.length < 3 || !Double.isFinite(candidate[0]) || !Double.isFinite(candidate[1])
                || !Double.isFinite(candidate[2]) || candidate[2] < 0.0D || candidate[2] > 1.0D) {
            throw new IllegalStateException("ImageFinder returned an invalid MATCH_TEMPLATE candidate");
        }
        int localCenterX = checkedRound(candidate[0], "match centerX");
        int localCenterY = checkedRound(candidate[1], "match centerY");
        int localLeft = checkedRound(candidate[0] - template.getWidth() / 2.0D, "match rectangle.x");
        int localTop = checkedRound(candidate[1] - template.getHeight() / 2.0D, "match rectangle.y");
        int centerX = Math.addExact(capturedRegion.x(), localCenterX);
        int centerY = Math.addExact(capturedRegion.y(), localCenterY);
        int left = Math.addExact(capturedRegion.x(), localLeft);
        int top = Math.addExact(capturedRegion.y(), localTop);
        TurnRegion rectangle = new TurnRegion(left, top, template.getWidth(), template.getHeight());
        return new TurnMatchResult(true, candidate[2], centerX, centerY, rectangle);
    }

    private static int checkedRound(double value, String field) {
        long rounded = Math.round(value);
        if (rounded < Integer.MIN_VALUE || rounded > Integer.MAX_VALUE) {
            throw new IllegalStateException(field + " exceeds the integer coordinate range");
        }
        return (int) rounded;
    }

    /**
     * Local mechanical result consumed later by TURN-11 action composition.
     *
     * @param match typed absolute match result; a miss contains no coordinates
     * @param clickRequested true only for a found match whose payload declared {@code onMatch=CLICK}
     * @param frame the already captured MATCH_EVIDENCE frame when requested, otherwise null
     */
    public record Execution(TurnMatchResult match, boolean clickRequested, TurnFrame frame) {

        public Execution {
            Objects.requireNonNull(match, "match");
            if (clickRequested && !match.found()) {
                throw new IllegalArgumentException("a missed template cannot request a click");
            }
        }
    }
}
