package com.bot.dhxy.window.observation;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Writes a bounded exact Tracker ROI when the local 五环 title template misses. */
@Slf4j
final class WuhuanTitleMatchMissDump {

    private static final Path DIR = Path.of("images", "temp", "match-miss", "wuhuan-title");
    private static final int LIMIT = 20;
    private static final AtomicInteger WRITTEN = new AtomicInteger();

    private WuhuanTitleMatchMissDump() {
    }

    static void write(byte[] png, String windowId, String taskRunId, long observerSeq, double score) {
        if (png == null || png.length == 0) {
            return;
        }
        int index = WRITTEN.incrementAndGet();
        if (index > LIMIT) {
            return;
        }
        try {
            Files.createDirectories(DIR);
            Path target = DIR.resolve(String.format("%02d-window-%s-run-%s-seq-%d-score-%.4f.png",
                    index, safe(windowId), safe(taskRunId), observerSeq, score));
            Files.write(target, png);
            log.info("[wuhuan-title] local title miss ROI written: score={} -> {}",
                    String.format("%.4f", score), target.toAbsolutePath());
        } catch (IOException | RuntimeException failure) {
            log.warn("[wuhuan-title] could not write title miss ROI: {}", failure.toString());
        }
    }

    private static String safe(String value) {
        return value == null ? "none" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
