package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G157（G145 P1+P2 收尾，2026-09-05 用户拍板"两个都做"）：
 * P2 收窄版时延——只有产生上行封包的时延可见，统一在 InputActionWorker（所有真实输入序列的
 * 唯一执行闸，纯本地轮询不经过）套对数正态乘子；
 * P1 落点高斯化（半项）——均匀方块改截断高斯椭圆，轨迹半边已判作废。
 */
class G157PacketRhythmAndGaussianContractTest {

    private static final Path WORKER = Path.of(
            "src/main/java/com/bot/dhxy/input/action/InputActionWorker.java");
    private static final Path FIFO_EXECUTOR = Path.of(
            "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java");
    private static final Path CLOUD_NPC_CLICK = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java");

    @Test
    void inputWorkerHumanizesEveryPacketAdjacentDelay() throws Exception {
        String source = Files.readString(WORKER, StandardCharsets.UTF_8);
        assertTrue(source.contains("clickLeft(action.getX(), action.getY(), humanizedDelayMs(action.getDelayMs()))"),
                "左键按住时长必须去精确化——恒定 150ms 按住是节律签名");
        assertTrue(source.contains("clickRight(action.getX(), action.getY(), humanizedDelayMs(action.getDelayMs()))"),
                "右键按住时长同理");
        assertTrue(source.contains("humanizedDelayMs(action.getDelayMs()), humanizedDelayMs(action.getIntervalMs())"),
                "双击间隔同理");
        assertTrue(source.contains("int humanizedSleepMs = humanizedDelayMs(action.getDelayMs());"),
                "序列步间 SLEEP 必须去精确化——这是服务端可算的操作间隔主体");
        assertTrue(source.contains("Math.max(0.8D, Math.min(1.5D, factor))"),
                "乘子必须有硬截断，防长尾拖垮时序契约");
    }

    @Test
    void humanizedDelayDistributionIsBoundedAndActuallyVaries() throws Exception {
        Method m = Class.forName("com.bot.dhxy.input.action.InputActionWorker")
                .getDeclaredMethod("humanizedDelayMs", int.class);
        m.setAccessible(true);
        int base = 150;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        java.util.Set<Integer> distinct = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            int v = (int) m.invoke(null, base);
            min = Math.min(min, v);
            max = Math.max(max, v);
            distinct.add(v);
        }
        assertTrue(min >= (int) (base * 0.8) && max <= (int) Math.ceil(base * 1.5) + 1,
                "抖后时延必须落在 [0.8x, 1.5x] 截断内: min=" + min + " max=" + max);
        assertTrue(distinct.size() >= 30, "时延必须真的在变（熵不能为零）: distinct=" + distinct.size());
        assertTrue((int) m.invoke(null, 0) == 0, "零/负时延原样返回，不许被抖出等待");
    }

    @Test
    void clickJitterIsTruncatedGaussianNotUniformSquare() throws Exception {
        String fifo = Files.readString(FIFO_EXECUTOR, StandardCharsets.UTF_8);
        assertTrue(fifo.contains("gaussianOffset(NPC_CLICK_JITTER_SIGMA_X, NPC_CLICK_JITTER_X)"),
                "FIFO 执行落点必须走截断高斯（均匀方块在热力图上是格子）");
        assertTrue(fifo.contains("insideAllowedRegion(jittered, spec) ? jittered : base"),
                "安全区回落语义不许被高斯化顺手改掉");
        String cloud = Files.readString(CLOUD_NPC_CLICK, StandardCharsets.UTF_8);
        assertTrue(cloud.contains("gaussianClickOffset(2.2D, 5)") && cloud.contains("gaussianClickOffset(1.8D, 4)"),
                "云端学习点重放同为截断高斯，且截断在既有 ±5/±4 边界内");
    }

    @Test
    void gaussianOffsetIsCenterHeavyWithinBounds() throws Exception {
        Method m = Class.forName("com.bot.dhxy.cloud.turn.local.NpcArrivalFrameFifoLocalExecutor")
                .getDeclaredMethod("gaussianOffset", double.class, int.class);
        m.setAccessible(true);
        int inner = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            int v = (int) m.invoke(null, 2.2D, 5);
            assertTrue(v >= -5 && v <= 5, "偏移越界: " + v);
            if (Math.abs(v) <= 2) {
                inner++;
            }
        }
        // 均匀分布 |v|<=2 的占比 = 5/11 ≈ 45%；σ=2.2 截断高斯 ≈ 71%。取 60% 为判别线。
        double innerRatio = inner / (double) samples;
        assertTrue(innerRatio > 0.60D,
                "落点必须中心密边缘稀（高斯形），不是均匀方块: innerRatio=" + innerRatio);
    }
}
