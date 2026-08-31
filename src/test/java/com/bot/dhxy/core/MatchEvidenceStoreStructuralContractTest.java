package com.bot.dhxy.core;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G112 复审合同（2026-08-27）：{@link MatchEvidenceStore} 的四条落盘语义。
 *
 * <p>结构化判定（对话框边框存在性这类）没有模板也没有命中坐标，旧的模板通道会在
 * {@code templateSupplier == null} 的第一道门直接 return——G112 第一版就是这样写了一行
 * 永远不产出的取证。本合同把两条通道各自的 PRESENT/ABSENT 钉死，并保留"无模板且非结构化
 * 一律不落盘"这条护栏，防止以后有人把 null 模板当普通调用混进来。</p>
 */
class MatchEvidenceStoreStructuralContractTest {

    private static final Path ROOT = Path.of("images/temp/match-evidence");
    private static final long WAIT_MS = 10_000L;

    /** 结构化通道：PRESENT 与 ABSENT 都必须落盘，分数记 na。 */
    @Test
    void structuralChannelWritesBothPresentAndAbsent() throws Exception {
        String site = "contract-structural-" + System.nanoTime();
        try {
            BufferedImage frame = frame(Color.DARK_GRAY);
            MatchEvidenceStore.saveStructural(site, "hwnd-TEST", frame, true);
            MatchEvidenceStore.saveStructural(site, "hwnd-TEST", frame, false);
            List<String> written = awaitFiles(site, 2);
            assertEquals(1, written.stream().filter(name -> name.contains("_PRESENT_score-na")).count(),
                    "structural PRESENT must land with an na score: " + written);
            assertEquals(1, written.stream().filter(name -> name.contains("_ABSENT_score-na")).count(),
                    "structural ABSENT must land with an na score: " + written);
            assertTrue(written.stream().allMatch(name -> name.contains("hwnd-TEST")),
                    "structural evidence must carry the windowId: " + written);
        } finally {
            deleteSite(site);
        }
    }

    /** 模板通道：命中带业务分数，miss 记 ABSENT + na——G112 的改动不得动到它。 */
    @Test
    void templateChannelKeepsItsScoreSemantics() throws Exception {
        String site = "contract-template-" + System.nanoTime();
        try {
            BufferedImage frame = frame(Color.DARK_GRAY);
            BufferedImage template = frame(Color.WHITE);
            MatchEvidenceStore.save(site, "hwnd-TEST", frame, template,
                    new double[]{40.0D, 30.0D, 0.913D});
            MatchEvidenceStore.save(site, "hwnd-TEST", frame, template, null);
            List<String> written = awaitFiles(site, 2);
            assertEquals(1, written.stream().filter(name -> name.contains("_PRESENT_score-0.913")).count(),
                    "a template hit must record the business score: " + written);
            assertEquals(1, written.stream().filter(name -> name.contains("_ABSENT_score-na")).count(),
                    "a template miss must record ABSENT with no invented score: " + written);
        } finally {
            deleteSite(site);
        }
    }

    /** 护栏：非结构化调用传 null 模板依旧一张都不落，避免"写了却永远不产出"再次发生。 */
    @Test
    void templateChannelStillRefusesANullTemplate() throws Exception {
        String site = "contract-null-template-" + System.nanoTime();
        try {
            MatchEvidenceStore.save(site, "hwnd-TEST", frame(Color.DARK_GRAY), null,
                    new double[]{40.0D, 30.0D, 0.913D});
            Thread.sleep(1_500L);
            assertFalse(Files.isDirectory(ROOT.resolve(site)),
                    "a null template on the template channel must stay a no-op");
        } finally {
            deleteSite(site);
        }
    }

    private static BufferedImage frame(Color fill) {
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(fill);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            g.dispose();
        }
        return image;
    }

    /** 编码与写盘在后台线程上，按名字轮询等待，超时即判失败。 */
    private static List<String> awaitFiles(String site, int expected) throws Exception {
        Path dir = ROOT.resolve(site);
        long deadline = System.currentTimeMillis() + WAIT_MS;
        List<String> names = List.of();
        while (System.currentTimeMillis() < deadline) {
            names = listStamped(dir);
            if (names.size() >= expected) {
                return names;
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("expected " + expected + " evidence files under " + dir
                + " but saw " + names);
    }

    private static List<String> listStamped(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".png") && !name.startsWith("latest"))
                    .sorted()
                    .toList();
        }
    }

    private static void deleteSite(String site) throws IOException {
        Path dir = ROOT.resolve(site);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
