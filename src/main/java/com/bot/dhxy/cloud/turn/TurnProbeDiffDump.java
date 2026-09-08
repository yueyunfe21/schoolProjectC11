package com.bot.dhxy.cloud.turn;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * G143（用户裁定 2026-09-02：全项目图片判定必须留证）：turn PROBE 帧差判定的取证池。
 *
 * <p>PROBE 用 before/after 两帧的像素差回答"刚才那一下点击/悬停到底改变了画面没有"
 * （PIXELS_CHANGED / PIXELS_UNCHANGED），云端拿这个判定决定重试还是推进。此前两帧在判定后
 * 直接丢弃——判错了（阈值不合适、截屏时机不对、别的窗口内容漂进来）没有任何图可对质。</p>
 *
 * <p>滚动池：before|after 并排合成一张，固定 {@value #POOL} 张滚动覆盖，磁盘占用恒定。
 * 留证永不影响判定：任何异常只落一行 warn。</p>
 */
@Slf4j
final class TurnProbeDiffDump {

    private static final Path DIR = Path.of("images", "temp", "match-evidence", "turn-probe-diff");
    private static final int POOL = 20;
    private static final AtomicInteger WRITTEN = new AtomicInteger();

    private TurnProbeDiffDump() {
    }

    /**
     * @param before 判定用的前帧（调用方随后自行 flush/丢弃）。
     * @param after 判定用的后帧。
     * @param unchanged 判定结果：true=PIXELS_UNCHANGED。
     * @param threshold 判定用的差异率阈值。
     */
    static void keep(BufferedImage before, BufferedImage after, boolean unchanged, double threshold) {
        try {
            if (before == null || after == null) {
                return;
            }
            int width = before.getWidth() + 4 + after.getWidth();
            int height = Math.max(before.getHeight(), after.getHeight());
            BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = composite.createGraphics();
            try {
                g.drawImage(before, 0, 0, null);
                g.drawImage(after, before.getWidth() + 4, 0, null);
            } finally {
                g.dispose();
            }
            Files.createDirectories(DIR);
            int index = WRITTEN.getAndIncrement() % POOL;
            String name = String.format(Locale.ROOT, "%02d_%s_thr-%.3f.png",
                    index, unchanged ? "UNCHANGED" : "CHANGED", threshold);
            ImageIO.write(composite, "png", DIR.resolve(name).toFile());
        } catch (Throwable failure) {
            log.warn("[turn-probe-diff] evidence keep failed: {}", failure.toString());
        }
    }
}
