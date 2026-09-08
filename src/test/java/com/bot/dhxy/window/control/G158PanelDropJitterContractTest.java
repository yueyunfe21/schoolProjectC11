package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G158（2026-09-05 用户拍板"浮动可以大,100 都能接受"）：两处面板拖回落点去同一化。
 * 固定落点使五窗面板永远停在同一像素（纯本地目视面签名）。
 * 硬约束：①自动面板的对齐判据(>20px)与拖拽落点必须同源用抖后点——否则场场重拖；
 * ②追踪面板落点截断必须收在默认 ROI 盒(±100/±75)内侧——否则拖完又触发；
 * ③自动面板 y 向下截断 ≤+10——底边 754 距窗底 768 仅 14px 余量。
 */
class G158PanelDropJitterContractTest {

    private static final Path SAMPLER = Path.of(
            "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java");
    private static final Path TRACKER_MECHANICS = Path.of(
            "src/main/java/com/bot/dhxy/cloud/turn/local/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java");

    @Test
    void autoPanelCheckAndDropShareTheSameJitteredPoint() throws Exception {
        String source = Files.readString(SAMPLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("int targetX = frameRect[0] + autoPanelSafeJitteredX;"),
                "对齐判据与拖拽落点必须同用本窗抖后安全点——参照点不抖会场场重拖");
        assertTrue(source.contains("int targetY = frameRect[1] + autoPanelSafeJitteredY;"),
                "y 同理");
        assertTrue(source.contains("truncatedGaussianOffset(45.0D, -100, 100)"),
                "x 截断 ±100（用户拍板上限）");
        assertTrue(source.contains("truncatedGaussianOffset(30.0D, -80, 10)"),
                "y 向下截断必须 ≤+10——窗底几何余量仅 14px");
        assertTrue(source.contains("private final int autoPanelSafeJitteredX"),
                "抖后点必须是每窗实例字段（窗内稳定、窗间不同），不许每次重掷");
        // G158 v2：用户拍板 50px 内不搬家（原 20 太敏感）。
        assertTrue(source.contains("AUTO_PANEL_ALIGN_TOLERANCE_PX = 50.0"),
                "对齐容差须为用户拍板的 50px，不许悄悄回 20");
    }

    @Test
    void trackerDropStaysInsideTheDefaultRoiBox() throws Exception {
        String source = Files.readString(TRACKER_MECHANICS, StandardCharsets.UTF_8);
        assertTrue(source.contains("DRAG_TARGET_X + truncatedGaussianOffset(35.0D, -80, 80)"),
                "追踪面板 x 截断 ±80——ROI 盒 ±100 内侧留 20 余量");
        assertTrue(source.contains("DRAG_TARGET_Y + truncatedGaussianOffset(25.0D, -60, 60)"),
                "追踪面板 y 截断 ±60——ROI 盒 ±75 内侧留 15 余量");
        assertTrue(source.contains("anchor = new Point(dropX, dropY);"),
                "拖后锚点必须记抖后实际落点，不许记旧固定点");
        assertTrue(source.contains("LOCAL_LEFT = -100") && source.contains("LOCAL_BOTTOM = 75"),
                "ROI 盒常量是余量计算的出处，悄改须同步截断与本合同");
    }

    @Test
    void jitterOffsetsAreBoundedAndActuallyVary() throws Exception {
        Method m = Class.forName("com.bot.dhxy.window.observation.WindowObservationSampler")
                .getDeclaredMethod("truncatedGaussianOffset", double.class, int.class, int.class);
        m.setAccessible(true);
        java.util.Set<Integer> distinct = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            int v = (int) m.invoke(null, 45.0D, -100, 100);
            assertTrue(v >= -100 && v <= 100, "x 偏移越界: " + v);
            distinct.add(v);
        }
        assertTrue(distinct.size() >= 50, "落点必须真的在散（熵不为零）: distinct=" + distinct.size());
        for (int i = 0; i < 20_000; i++) {
            int v = (int) m.invoke(null, 30.0D, -80, 10);
            assertTrue(v >= -80 && v <= 10, "y 偏移越出几何硬约束: " + v);
        }
    }
}
