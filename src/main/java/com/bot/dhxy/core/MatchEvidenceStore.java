package com.bot.dhxy.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 用户铁律（2026-08-17 定，2026-08-18 全量清扫）：所有模板匹配点必须落盘判定原图。
 *
 * <p>统一落盘工具：原帧 + 业务判定的匹配结果 + 标注框写到 {@code images/temp/match-evidence/<site>/}，
 * 文件名自带时间戳、windowId、PRESENT/ABSENT 与分数。任何失败只记 warn，绝不影响调用方的
 * 判定语义；本类不改变、不重算任何业务判定结果。</p>
 *
 * <ul>
 *   <li>{@link #save}：决策点用——每次判定一张时间戳文件。</li>
 *   <li>{@link #saveOnChange}：高频探测（观察循环/每拍 tick）用——latest 文件始终覆盖写，
 *       PRESENT/ABSENT 翻转时才另存时间戳文件，防止刷盘。</li>
 *   <li>{@link #saveOnChangeLazy}：帧/模板需要昂贵获取（磁盘解码）的调用点用——先做节流判定，
 *       决定要落盘了才向 supplier 要图。</li>
 * </ul>
 *
 * <p><b>2026-08-21 性能返修（整机卡顿取证后）：</b></p>
 * <ul>
 *   <li><b>不再二次匹配。</b>旧实现每次落盘都 {@code ImageFinder.find(frame, template, -1.0)}
 *       重新全量匹配一遍"纯为打分"——含每 5 秒一次的 latest 重写，等于每个 site×窗口静默地
 *       把业务匹配白做一遍。分数与标注框现在只取调用方传入的业务判定结果；miss 无坐标就不画框，
 *       分数记 na。证据的语义（判定当时的原图 + 判定自己的结果）反而更忠实。</li>
 *   <li><b>PNG 编码与写盘移出调用线程。</b>标注图在调用线程上合成（廉价，且必须在调用方 flush
 *       原帧之前完成），编码+写盘交给单条低优先后台线程；有界队列，满则丢弃并计数——证据是
 *       尽力而为，绝不允许它反过来拖慢感知与输入（E41 同族教训）。</li>
 * </ul>
 */
public final class MatchEvidenceStore {

    private static final Logger log = LoggerFactory.getLogger(MatchEvidenceStore.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final ConcurrentHashMap<String, Boolean> LAST_PRESENT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> LAST_LATEST_WRITE_AT = new ConcurrentHashMap<>();
    /*
     * 2026-08-20 事故修正：latest 覆盖写曾经每拍都编码+写盘，战斗信号等高频点（6 点×5 窗×每秒
     * 多拍）把客户端截图管线拖出 20 秒级卡顿（08:23 清杂窗 20.2s 实证，一天 1.5 万张图）。
     * 现在：结果未翻转时 latest 至多每 5 秒重写一次，且不写盘的拍连图都不向 supplier 要（零开销早退）。
     */
    private static final long LATEST_REWRITE_INTERVAL_MS = 5_000L;
    /*
     * G103（2026-08-25 用户确认）：常态运行不再做 latest 周期重写——战斗期间 6 站点×5 窗的
     * 5 秒级 latest 刷新实测每分钟 126-200 张 PNG 编码+写盘，是截图风暴的次级放大项。
     * 判定链留痕铁律（2026-08-17）不受影响：PRESENT/ABSENT 翻转的时间戳存档、决策点 save()
     * 全量保留，latest 也仍在每次翻转时更新。需要连续 latest 流时以
     * -Ddhxy.matchEvidence.latestRefresh=true 启动即可恢复旧行为。
     */
    private static final boolean LATEST_PERIODIC_REFRESH_ENABLED =
            Boolean.getBoolean("dhxy.matchEvidence.latestRefresh");

    /**
     * 编码+写盘专用单线程：最低优先级、守护线程、有界队列。队列满 = 磁盘/CPU 已经跟不上取证
     * 节奏，此时丢新证据、保感知流畅（丢弃计数定期告警，绝不静默）。
     */
    private static final ThreadPoolExecutor WRITER = new ThreadPoolExecutor(
            1, 1, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            runnable -> {
                Thread thread = new Thread(runnable, "dhxy-match-evidence-writer");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());
    private static final AtomicLong DROPPED = new AtomicLong();

    private MatchEvidenceStore() {
    }

    /**
     * 每次判定落盘一张时间戳文件。
     *
     * @param site 匹配点短名（kebab-case，作子目录名）
     * @param windowId 窗口 id，未知传 null
     * @param frame 参与判定的原帧（调用方 flush 前调用）
     * @param template 参与判定的模板
     * @param thresholdMatch 业务判定用的原始匹配结果（命中数组或 null），原样传入，不重算
     */
    public static void save(String site,
                            String windowId,
                            BufferedImage frame,
                            BufferedImage template,
                            double[] thresholdMatch) {
        write(site, windowId, frame == null ? null : () -> frame,
                template == null ? null : () -> template, thresholdMatch, false, false);
    }

    /** 高频探测节流版：latest 覆盖写；PRESENT/ABSENT 翻转时另存时间戳文件。 */
    public static void saveOnChange(String site,
                                    String windowId,
                                    BufferedImage frame,
                                    BufferedImage template,
                                    double[] thresholdMatch) {
        write(site, windowId, frame == null ? null : () -> frame,
                template == null ? null : () -> template, thresholdMatch, true, false);
    }

    /**
     * 节流版的懒加载变体：先做键/翻转/间隔判定，确定要落盘了才调用 supplier 取图。
     * 供"取图本身昂贵"（如需从磁盘重新解码 PNG）的调用点使用；supplier 返回 null 视为放弃。
     */
    public static void saveOnChangeLazy(String site,
                                        String windowId,
                                        Supplier<BufferedImage> frame,
                                        Supplier<BufferedImage> template,
                                        double[] thresholdMatch) {
        write(site, windowId, frame, template, thresholdMatch, true, true);
    }

    private static void write(String site,
                              String windowId,
                              Supplier<BufferedImage> frameSupplier,
                              Supplier<BufferedImage> templateSupplier,
                              double[] thresholdMatch,
                              boolean throttled,
                              boolean ownsImages) {
        try {
            if (frameSupplier == null || templateSupplier == null || site == null || site.isBlank()) {
                return;
            }
            boolean present = thresholdMatch != null && thresholdMatch.length >= 3;
            String window = resolveWindowKey(windowId);
            String stateKey = site + "|" + window;
            Boolean previous = LAST_PRESENT.put(stateKey, present);
            boolean flipped = previous == null || previous != present;
            if (throttled && !flipped) {
                // G103：常态运行未翻转的拍一律零开销早退——latest 只在翻转时更新。
                // 调试开关打开时恢复"至多每 LATEST_REWRITE_INTERVAL_MS 重写一次"的旧节流。
                if (!LATEST_PERIODIC_REFRESH_ENABLED) {
                    return;
                }
                long now = System.currentTimeMillis();
                Long lastAt = LAST_LATEST_WRITE_AT.get(stateKey);
                if (lastAt != null && now - lastAt < LATEST_REWRITE_INTERVAL_MS) {
                    return;
                }
            }

            BufferedImage frame = frameSupplier.get();
            BufferedImage template = templateSupplier.get();
            if (frame == null || template == null) {
                return;
            }

            /*
             * 分数与标注框只来自业务判定自己的结果。miss（thresholdMatch==null）没有坐标，
             * 不画框、分数 na——这是忠实记录，而不是替业务再算一个它没用过的分。
             */
            double score = present && Double.isFinite(thresholdMatch[2]) ? thresholdMatch[2] : Double.NaN;

            // 标注图必须在调用线程合成：返回后调用方随时可能 flush 原帧。合成只是一次内存拷贝
            // + 画一个框，远比 PNG 编码便宜；昂贵的编码与写盘从这里开始交给后台线程。
            BufferedImage marked = new BufferedImage(
                    frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = marked.createGraphics();
            try {
                g.drawImage(frame, 0, 0, null);
                if (present && Double.isFinite(thresholdMatch[0]) && Double.isFinite(thresholdMatch[1])) {
                    g.setColor(Color.GREEN);
                    int w = template.getWidth();
                    int h = template.getHeight();
                    int x = (int) Math.round(thresholdMatch[0]) - w / 2;
                    int y = (int) Math.round(thresholdMatch[1]) - h / 2;
                    g.drawRect(Math.max(0, Math.min(x, frame.getWidth() - 1)),
                            Math.max(0, Math.min(y, frame.getHeight() - 1)), w, h);
                }
            } finally {
                g.dispose();
                if (ownsImages) {
                    // 懒加载路径：解码出来的图归本类所有，标注图合成完毕即释放。
                    frame.flush();
                    template.flush();
                }
            }

            String scoreText = Double.isNaN(score) ? "na" : String.format("%.3f", score);
            boolean writeLatest = throttled;
            boolean writeStamped = !throttled || flipped;
            String stampedName = writeStamped
                    ? LocalDateTime.now().format(STAMP)
                            + "_" + window
                            + "_" + (present ? "PRESENT" : "ABSENT")
                            + "_score-" + scoreText
                            + ".png"
                    : null;
            if (writeLatest) {
                LAST_LATEST_WRITE_AT.put(stateKey, System.currentTimeMillis());
            }
            enqueueWrite(site, window, present, scoreText, marked, writeLatest, stampedName);
        } catch (Throwable failure) {
            log.warn("[match-evidence] save failed: site={} reason={}", site, failure.toString());
        }
    }

    /** 编码+写盘的后台部分；{@code marked} 的所有权自此归 writer 线程。 */
    private static void enqueueWrite(String site,
                                     String window,
                                     boolean present,
                                     String scoreText,
                                     BufferedImage marked,
                                     boolean writeLatest,
                                     String stampedName) {
        try {
            WRITER.execute(() -> {
                try {
                    Path dir = Path.of("images", "temp", "match-evidence", site);
                    Files.createDirectories(dir);
                    if (writeLatest) {
                        ImageIO.write(marked, "png", dir.resolve("latest_" + window + ".png").toFile());
                    }
                    if (stampedName != null) {
                        ImageIO.write(marked, "png", dir.resolve(stampedName).toFile());
                        log.info("[match-evidence] saved: site={} windowId={} present={} score={} file={}",
                                site, window, present, scoreText, stampedName);
                    }
                } catch (Throwable failure) {
                    log.warn("[match-evidence] async write failed: site={} reason={}",
                            site, failure.toString());
                } finally {
                    marked.flush();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException full) {
            marked.flush();
            long dropped = DROPPED.incrementAndGet();
            if (dropped % 100 == 1) {
                log.warn("[match-evidence] writer queue full; evidence dropped (total={}): site={}",
                        dropped, site);
            }
        }
    }

    /**
     * 2026-08-20 事故修正：调用方拿不到 windowId 时曾统一落 "unknown"，五个窗口共用一个节流键，
     * PRESENT/ABSENT 互相翻转把"翻转才存档"打穿成全速刷盘。观察/采样线程名自带 hwnd
     * （如 dhxy-observe-hwnd-XXXX），从线程名提取窗口标识兜底。
     */
    private static String resolveWindowKey(String windowId) {
        if (windowId != null && !windowId.isBlank()) {
            return windowId;
        }
        String thread = Thread.currentThread().getName();
        if (thread != null) {
            int at = thread.indexOf("hwnd-");
            if (at >= 0) {
                String tail = thread.substring(at);
                return tail.replaceAll("[^A-Za-z0-9_-]", "");
            }
        }
        return "unknown";
    }
}
