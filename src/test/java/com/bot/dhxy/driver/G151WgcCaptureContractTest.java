package com.bot.dhxy.driver;

import com.bot.dhxy.config.CaptureBackendProperties;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G151（G145 P8 升格，用户拍板 2026-09-04）：采集换底 WGC sidecar。
 * PrintWindow 是全系统唯一向游戏窗口投递重绘请求的采集路径；WGC 目标进程零参与。
 * 实测（3519）：帧契约同构 1036x783、像素零偏移、E2E 17~57ms/帧。
 */
class G151WgcCaptureContractTest {

    private static final Path CAPTURE_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java");

    @Test
    void defaultBackendStaysLegacyUntilExplicitlyEnabled() {
        assertEquals(CaptureBackendProperties.Backend.LEGACY,
                new CaptureBackendProperties().getBackend(),
                "默认必须 LEGACY——没配置本机 config 前行为零变化");
    }

    @Test
    void bgraConversionIsExact() throws Exception {
        Method convert = WgcSidecarCaptureClient.class.getDeclaredMethod(
                "toRgbImage", byte[].class, int.class, int.class);
        convert.setAccessible(true);
        // 2x2：纯蓝/纯绿/纯红/游戏文字绿(0,208,17)，BGRA 顶行在前
        byte[] bgra = new byte[]{
                (byte) 255, 0, 0, (byte) 255,   0, (byte) 255, 0, (byte) 255,
                0, 0, (byte) 255, (byte) 255,   17, (byte) 208, 0, (byte) 255,
        };
        BufferedImage image = (BufferedImage) convert.invoke(null, bgra, 2, 2);
        assertEquals(0x0000FF, image.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0x00FF00, image.getRGB(1, 0) & 0xFFFFFF);
        assertEquals(0xFF0000, image.getRGB(0, 1) & 0xFFFFFF);
        assertEquals(0x00D011, image.getRGB(1, 1) & 0xFFFFFF,
                "文字绿 (0,208,17) 必须逐通道无损——色盲匹配判据依赖精确色值");
    }

    @Test
    void sidecarUnavailableFallsBackToEmpty() {
        CaptureBackendProperties properties = new CaptureBackendProperties();
        properties.setWgcSidecarPort(47999);  // 无人监听的端口
        properties.setWgcTimeoutMs(200);
        WgcSidecarCaptureClient client = new WgcSidecarCaptureClient(properties);
        Optional<BufferedImage> frame = client.captureWindow(12345L, 1036, 783);
        assertTrue(frame.isEmpty(), "sidecar 不在线必须收敛为 empty（调用方回退 LEGACY 链），不许抛出");
    }

    @Test
    void printWindowIsFullyDeletedAndOnlyGameSilentProvidersRemain() throws Exception {
        String source = Files.readString(CAPTURE_SERVICE, StandardCharsets.UTF_8);
        // 用户拍板（2026-09-04"兜底我都不想要"）：PrintWindow 是唯一会让游戏窗口收到重绘请求的
        // 采集路径，可执行面必须为零——注释里的历史记载不算。
        assertTrue(!source.contains("User32.INSTANCE.PrintWindow"),
                "PrintWindow 调用必须整体删除，任何兜底复活都是回归");
        assertTrue(!source.contains("PRINT_WINDOW_RENDER_FULL_CONTENT"),
                "PW_RENDERFULLCONTENT 常量必须随调用一并删除");
        assertTrue(!source.contains("HWND_PRINTWINDOW,"),
                "CaptureProvider 枚举不许再有 PrintWindow 标签");
        int wgcBranch = source.indexOf("Backend.WGC_SIDECAR");
        int bitBltCall = source.indexOf("GDI32.INSTANCE.BitBlt(memoryDc, 0, 0, width, height");
        assertTrue(wgcBranch > 0 && bitBltCall > 0 && wgcBranch < bitBltCall,
                "链序必须 WGC 优先、BitBlt 回退——两路都不向游戏窗口投递消息");
        assertTrue(source.contains("CaptureProvider.WGC_SIDECAR"),
                "WGC 帧必须带独立 provider 标签（落盘证据可归因）");
        assertTrue(source.contains("probeBlank(wgc.orElse(null))"),
                "WGC 帧同样要过空帧探测，黑帧不许当有效画面");
    }
}
