package com.bot.dhxy.window.observation;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep the frame a 天庭 dialog match was looked for in, when none of the templates matched.
 *
 * <p>A miss reported as an empty result cannot be diagnosed. "No template matched" is the same log line
 * whether the templates are wrong, the ROI is in the wrong place, or the frame was sampled before the
 * dialog had drawn — and those need three different fixes. The captured ROI separates them at a glance,
 * and without this it is flushed a line later.</p>
 *
 * <p>Bounded, so a run that never matches cannot fill the disk.</p>
 */
@Slf4j
final class DialogMatchMissDump {

    private static final Path DIR = Path.of("images", "temp", "match-miss", "tianting-dialog");
    private static final int LIMIT = 20;
    private static final AtomicInteger WRITTEN = new AtomicInteger();

    private DialogMatchMissDump() {
    }

    static void write(BufferedImage roi, String bestTemplatePath, double bestScore) {
        if (roi == null) {
            return;
        }
        int index = WRITTEN.incrementAndGet();
        if (index > LIMIT) {
            return;
        }
        try {
            Files.createDirectories(DIR);
            String templateName = bestTemplatePath == null ? "none"
                    : bestTemplatePath.replaceAll(".*/", "").replace(".png", "");
            Path target = DIR.resolve(
                    String.format("%02d-best-%s-score-%.4f.png", index, templateName, bestScore));
            ImageIO.write(roi, "png", target.toFile());
            log.info("[tianting-dialog] miss ROI written: best={} score={} -> {}",
                    bestTemplatePath, String.format("%.4f", bestScore), target.toAbsolutePath());
        } catch (IOException | RuntimeException failure) {
            // Evidence is worth trying for, never worth failing an observation cycle over.
            log.warn("[tianting-dialog] could not write the miss ROI: {}", failure.toString());
        }
    }
}
