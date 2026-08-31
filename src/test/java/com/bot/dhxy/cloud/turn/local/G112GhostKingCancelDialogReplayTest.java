package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.window.observation.DialogFramePresenceMechanics;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G112 复审回放（2026-08-27）：用真实窗口帧证明“取消模板没命中 != 对话框没开”。
 *
 * <p>21:00 事故的判据错误是：客户端 executeCancel 只看 quxiao.png，别的对话框开着也返回
 * NOT_MATCHED，Cloud 据此再点 NPC，把点击打进已经开着的框里。本回放对每张帧同时求出
 * ①窄 ROI 里的取消模板是否命中 ②最大对话框 ROI 的结构化边框是否存在，喂进生产的
 * {@link GhostKingDialogLocalOperation#mapCancelOutcome}，并断言三条视觉分支各自复现：</p>
 *
 * <ul>
 *   <li>{@code cancelled--*}：21:01 事故原帧——鬼王取消框真的开着，取消模板命中，
 *       生产会点掉取消选项而<b>不</b>点 NPC；</li>
 *   <li>{@code dialog-without-cancel--*}：别的对话框开着但没有鬼王取消选项（含南瓜任务的取消框），
 *       必须 fail-closed，NPC 点击 0 次；</li>
 *   <li>{@code not-matched--*}：裸场景，才允许重开一次 NPC。</li>
 * </ul>
 *
 * <p>语料分两层：{@code FIXTURES} 是随仓库入库的五张固定帧，任何 clone 都能跑；
 * {@code CORPUS_ROOT} 是本机完整对话框语料（未入库，38MB），存在时额外全量扫一遍。
 * 两层都会把带标注的判定图写进 {@code MARKED_OUTPUT}。</p>
 */
class G112GhostKingCancelDialogReplayTest {

    private static final int FRAME_X = 200;
    private static final int FRAME_Y = 250;
    private static final int FRAME_W = 640;
    private static final int FRAME_H = 300;
    private static final int CANCEL_X = 250;
    private static final int CANCEL_Y = 312;
    private static final int CANCEL_W = 529;
    private static final int CANCEL_H = 208;
    private static final double MATCH_RATE = 0.82D;
    private static final String CANCEL_TEMPLATE = "images/template/dialog/guiwang/quxiao.png";
    /** 随仓库入库的最小语料：文件名前缀即期望判定。 */
    private static final Path FIXTURES = Path.of("src/test/resources/images/test-cases/g112");
    /** 本机完整语料，未入库；存在才扫。 */
    private static final Path CORPUS_ROOT = Path.of(System.getProperty(
            "dhxy.dialog.frame.corpus", "images/test-cases/dialog-frame-classification"));
    /** G112 专属 marked 输出（生成物，走 images/temp，不入库）。 */
    private static final Path MARKED_OUTPUT = Path.of("images/temp/match-evidence/g112-cancel-replay");

    /** 入库语料：任何 clone 都必须复现这五条判定，且三条视觉分支都被覆盖。 */
    @Test
    void trackedFixturesPinEveryCancelPhaseVerdict() throws Exception {
        assertTrue(Files.isDirectory(FIXTURES), "missing tracked G112 fixtures: " + FIXTURES);
        List<Path> fixtures;
        try (Stream<Path> stream = Files.list(FIXTURES)) {
            fixtures = stream.filter(path -> path.toString().endsWith(".png"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        assertEquals(5, fixtures.size(), "the tracked fixture set must stay at five frames");
        Map<GhostKingDialogLocalOperation.CancelResult, Integer> seen =
                new EnumMap<>(GhostKingDialogLocalOperation.CancelResult.class);
        for (Path fixture : fixtures) {
            GhostKingDialogLocalOperation.CancelResult expected = expectedFrom(fixture);
            Replay replay = replay(fixture, "fixture");
            assertEquals(expected, replay.result(),
                    "tracked fixture verdict changed: " + fixture.getFileName());
            seen.merge(replay.result(), 1, Integer::sum);
        }
        assertEquals(1, seen.getOrDefault(GhostKingDialogLocalOperation.CancelResult.CANCELLED, 0),
                "the 21:01 incident frame must reproduce the real CANCELLED branch");
        assertEquals(2, seen.getOrDefault(
                        GhostKingDialogLocalOperation.CancelResult.DIALOG_WITHOUT_CANCEL, 0),
                "two tracked frames must carry an open dialog without the ghost-king cancel option");
        assertEquals(2, seen.getOrDefault(GhostKingDialogLocalOperation.CancelResult.NOT_MATCHED, 0),
                "two tracked frames must be bare scenes");
    }

    /** 21:01 事故原帧：取消框确实开着，生产算法必须命中取消模板并给出点击点。 */
    @Test
    void incidentFrameMatchesTheCancelTemplateInsteadOfReopeningTheNpc() throws Exception {
        Path incident = FIXTURES.resolve("cancelled--incident-2101-ghost-king-cancel-dialog.png");
        assertTrue(Files.isRegularFile(incident), "missing incident fixture: " + incident);
        Replay replay = replay(incident, "incident");
        assertEquals(GhostKingDialogLocalOperation.CancelResult.CANCELLED, replay.result());
        assertNotNull(replay.match(), "the incident frame must expose a real template hit");
        assertTrue(replay.match()[2] >= MATCH_RATE,
                "incident score must clear the production threshold: " + replay.match()[2]);
        // 事故当时生产点的是固定点 (580,479)，落在已经开着的对话框选项区；
        // 取消模板的命中中心才是这一帧唯一该点的地方。
        double clickX = CANCEL_X + replay.match()[0];
        double clickY = CANCEL_Y + replay.match()[1];
        assertTrue(clickX >= CANCEL_X && clickX <= CANCEL_X + CANCEL_W
                        && clickY >= CANCEL_Y && clickY <= CANCEL_Y + CANCEL_H,
                "the cancel click must land inside the cancel ROI: " + clickX + "," + clickY);
    }

    /** 本机完整语料（未入库）：存在时全量扫，证明结论不是挑出来的五张。 */
    @Test
    void localCorpusSweepNeverReportsAnOpenDialogAsBareScene() throws Exception {
        if (!Files.isDirectory(CORPUS_ROOT)) {
            System.out.println("[G112 replay] local corpus absent, tracked fixtures already cover the contract: "
                    + CORPUS_ROOT.toAbsolutePath());
            return;
        }
        List<Path> cases = corpus();
        assertTrue(!cases.isEmpty(), "dialog-frame corpus is empty");
        int dialogWithoutCancel = 0;
        int cancelled = 0;
        int bare = 0;
        for (Path source : cases) {
            boolean dialogOpen = source.getParent().getFileName().toString().equals("positive");
            GhostKingDialogLocalOperation.CancelResult result = replay(source, "corpus").result();
            if (dialogOpen) {
                assertNotEquals(GhostKingDialogLocalOperation.CancelResult.NOT_MATCHED, result,
                        "an open dialog must never be reported as a bare scene: " + source.getFileName());
                if (result == GhostKingDialogLocalOperation.CancelResult.CANCELLED) {
                    cancelled++;
                } else {
                    dialogWithoutCancel++;
                }
            } else {
                assertEquals(GhostKingDialogLocalOperation.CancelResult.NOT_MATCHED, result,
                        "a bare scene must stay reopenable: " + source.getFileName());
                bare++;
            }
        }
        System.out.println("[G112 replay] corpus cancelled=" + cancelled
                + " dialogWithoutCancel=" + dialogWithoutCancel + " bare=" + bare
                + " marked=" + MARKED_OUTPUT.toAbsolutePath());
        assertTrue(dialogWithoutCancel > 0,
                "the corpus must contain at least one dialog that carries no cancel option");
        assertTrue(bare > 0, "the corpus must contain at least one bare scene");
    }

    /** 探针不可用时（截图拿不到）必须是 PRESENCE_UNKNOWN，Cloud 侧同样 fail-closed。 */
    @Test
    void unavailableFrameProbeIsNeverBareScene() {
        assertEquals(GhostKingDialogLocalOperation.CancelResult.PRESENCE_UNKNOWN,
                GhostKingDialogLocalOperation.mapCancelOutcome(false, null));
        assertEquals(GhostKingDialogLocalOperation.CancelResult.CANCELLED,
                GhostKingDialogLocalOperation.mapCancelOutcome(true, null));
    }

    private static GhostKingDialogLocalOperation.CancelResult expectedFrom(Path fixture) {
        String name = fixture.getFileName().toString();
        if (name.startsWith("cancelled--")) {
            return GhostKingDialogLocalOperation.CancelResult.CANCELLED;
        }
        if (name.startsWith("dialog-without-cancel--")) {
            return GhostKingDialogLocalOperation.CancelResult.DIALOG_WITHOUT_CANCEL;
        }
        if (name.startsWith("not-matched--")) {
            return GhostKingDialogLocalOperation.CancelResult.NOT_MATCHED;
        }
        throw new AssertionError("fixture name must encode its expected verdict: " + name);
    }

    /** 一张帧的判定结果与它的原始模板命中（miss 为 null）。 */
    private record Replay(GhostKingDialogLocalOperation.CancelResult result, double[] match) {
    }

    /**
     * 跑一张帧的完整判定链并落一张 G112 marked 图：青色=结构化边框 ROI，橙/绿色=取消模板 ROI，
     * 命中时另画绿色模板框与红色最终点击点，标题行写明两个事实与最终结论。
     */
    private Replay replay(Path source, String group) throws Exception {
        BufferedImage template = ImageIO.read(Path.of(CANCEL_TEMPLATE).toFile());
        assertNotNull(template, "missing cancel template: " + CANCEL_TEMPLATE);
        BufferedImage full = ImageIO.read(source.toFile());
        assertNotNull(full, "unreadable frame: " + source);
        assertTrue(full.getWidth() >= FRAME_X + FRAME_W && full.getHeight() >= FRAME_Y + FRAME_H,
                "frame smaller than the maximum dialog ROI: " + source);
        try {
            boolean framePresent = new DialogFramePresenceMechanics()
                    .isPresent(full.getSubimage(FRAME_X, FRAME_Y, FRAME_W, FRAME_H));
            double[] match = ImageFinder.find(
                    full.getSubimage(CANCEL_X, CANCEL_Y, CANCEL_W, CANCEL_H),
                    template, MATCH_RATE);
            GhostKingDialogLocalOperation.CancelResult result =
                    GhostKingDialogLocalOperation.mapCancelOutcome(match != null, framePresent);
            writeMarked(source, group, full, template, framePresent, match, result);
            System.out.println("[G112 replay] " + group + " " + source.getFileName()
                    + " framePresent=" + framePresent
                    + " cancelScore=" + (match == null ? "na" : String.format("%.8f", match[2]))
                    + " -> " + result);
            return new Replay(result, match);
        } finally {
            template.flush();
            full.flush();
        }
    }

    private void writeMarked(Path source,
                             String group,
                             BufferedImage full,
                             BufferedImage template,
                             boolean framePresent,
                             double[] match,
                             GhostKingDialogLocalOperation.CancelResult result) throws Exception {
        Files.createDirectories(MARKED_OUTPUT);
        BufferedImage marked = new BufferedImage(
                full.getWidth(), full.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(full, 0, 0, null);
            g.setStroke(new BasicStroke(2f));
            g.setColor(framePresent ? Color.CYAN : new Color(80, 80, 80));
            g.drawRect(FRAME_X, FRAME_Y, FRAME_W, FRAME_H);
            g.setColor(match != null ? Color.GREEN : Color.ORANGE);
            g.drawRect(CANCEL_X, CANCEL_Y, CANCEL_W, CANCEL_H);
            if (match != null) {
                int centreX = (int) Math.round(CANCEL_X + match[0]);
                int centreY = (int) Math.round(CANCEL_Y + match[1]);
                g.setColor(Color.GREEN);
                g.drawRect(centreX - template.getWidth() / 2, centreY - template.getHeight() / 2,
                        template.getWidth(), template.getHeight());
                // 红色 = 生产真正会点下去的那一点（取消模板命中中心）。
                g.setColor(Color.RED);
                g.drawLine(centreX - 12, centreY, centreX + 12, centreY);
                g.drawLine(centreX, centreY - 12, centreX, centreY + 12);
                g.drawString("click " + centreX + "," + centreY, centreX + 14, centreY - 6);
            }
            g.setColor(Color.WHITE);
            g.drawString("framePresent=" + framePresent
                    + " cancelScore=" + (match == null ? "na" : String.format("%.8f", match[2]))
                    + " -> " + result, 12, 20);
        } finally {
            g.dispose();
        }
        String name = group + "--" + result.name().toLowerCase(Locale.ROOT)
                + "--" + source.getFileName().toString().replace(".png", "") + "_marked.png";
        ImageIO.write(marked, "png", MARKED_OUTPUT.resolve(name).toFile());
        marked.flush();
    }

    private List<Path> corpus() throws Exception {
        try (Stream<Path> stream = Files.walk(CORPUS_ROOT, 2)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".png"))
                    .filter(path -> {
                        String parent = path.getParent().getFileName().toString();
                        return parent.equals("positive") || parent.equals("negative");
                    })
                    .filter(this::isRawWindowFrame)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private boolean isRawWindowFrame(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return false;
            }
            try {
                return image.getWidth() >= FRAME_X + FRAME_W && image.getHeight() >= FRAME_Y + FRAME_H;
            } finally {
                image.flush();
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
