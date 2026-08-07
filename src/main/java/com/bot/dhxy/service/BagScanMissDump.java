package com.bot.dhxy.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep the bag page an item template was looked for in, when it was not found.
 *
 * <p>The scan already writes its capture, but always to the same two names — {@code bag_scan.png} and
 * {@code bag_scan_current.png} — so the page that missed is overwritten by the next scan seconds later.
 * By the time anyone asks "was the item even on that page", the evidence is gone.</p>
 *
 * <p>Bounded, so a run that never finds the item cannot fill the disk.</p>
 */
@Slf4j
final class BagScanMissDump {

    private static final Path DIR = Path.of("images", "temp", "match-miss", "bag-scan");
    private static final int LIMIT = 20;
    private static final AtomicInteger WRITTEN = new AtomicInteger();

    private BagScanMissDump() {
    }

    /**
     * @param capturedPath the scan image that was just matched against.
     * @param templatePath the item template that did not match.
     * @param page 1-based bag page, or 0 for the currently visible one.
     */
    static void copy(String capturedPath, String templatePath, int page) {
        if (capturedPath == null) {
            return;
        }
        int index = WRITTEN.incrementAndGet();
        if (index > LIMIT) {
            return;
        }
        try {
            Path source = Path.of(capturedPath);
            if (!Files.exists(source)) {
                return;
            }
            Files.createDirectories(DIR);
            String templateName = templatePath == null ? "unknown"
                    : templatePath.replaceAll(".*/", "").replace(".png", "");
            Path target = DIR.resolve(String.format("%02d-page%d-%s.png", index, page, templateName));
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[bag] miss page kept: template={} -> {}", templatePath, target.toAbsolutePath());
        } catch (IOException | RuntimeException failure) {
            // Evidence is worth trying for, never worth failing a bag scan over.
            log.warn("[bag] could not keep the miss page: {}", failure.toString());
        }
    }
}
