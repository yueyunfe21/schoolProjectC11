package com.bot.dhxy.driver;

import com.bot.dhxy.config.CaptureBackendProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * G151：WGC sidecar 采集客户端（协议见 scripts/wgc_capture_sidecar.py 头注释）。
 *
 * <p>每次采集独立短连接（localhost，1~6 请求/秒量级，连接开销微秒级），无共享可变态，
 * 天然线程安全。任何异常都收敛为 {@code Optional.empty()}，由调用方回退 LEGACY 链。</p>
 */
@Slf4j
@Service
public class WgcSidecarCaptureClient {

    private final CaptureBackendProperties properties;

    /** 失败告警节流：sidecar 未起时每拍都 warn 会刷屏。 */
    private volatile long lastFailureWarnAtMs;

    public WgcSidecarCaptureClient(CaptureBackendProperties properties) {
        this.properties = properties;
    }

    /**
     * 采集一帧整窗画面。
     *
     * @param hwnd 目标窗口原生句柄值。
     * @param expectedWidth 注册宽度；帧尺寸不符即判失败（尺寸漂移交 G148 熔断处置，这里不猜）。
     * @param expectedHeight 注册高度。
     * @return BGRA 转好的 RGB 帧；sidecar 不可用/超时/尺寸不符时为空。
     */
    public Optional<BufferedImage> captureWindow(long hwnd, int expectedWidth, int expectedHeight) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", properties.getWgcSidecarPort()), 300);
            socket.setSoTimeout(properties.getWgcTimeoutMs() + 500);
            OutputStream out = socket.getOutputStream();
            out.write(("CAP " + hwnd + " " + properties.getWgcFreshMs() + " "
                    + properties.getWgcTimeoutMs() + "\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            String header = readLine(in);
            if (header == null || !header.startsWith("OK ")) {
                warnThrottled("wgc-sidecar refused capture: hwnd=" + hwnd + " reply=" + header);
                return Optional.empty();
            }
            String[] parts = header.trim().split(" ");
            int width = Integer.parseInt(parts[1]);
            int height = Integer.parseInt(parts[2]);
            int length = Integer.parseInt(parts[3]);
            if (length != width * height * 4 || length <= 0 || length > 64 * 1024 * 1024) {
                warnThrottled("wgc-sidecar frame length mismatch: " + header);
                return Optional.empty();
            }
            byte[] bgra = new byte[length];
            in.readFully(bgra);
            if (width != expectedWidth || height != expectedHeight) {
                warnThrottled("wgc-sidecar frame size differs from registered geometry: hwnd=" + hwnd
                        + " frame=" + width + "x" + height
                        + " registered=" + expectedWidth + "x" + expectedHeight);
                return Optional.empty();
            }
            return Optional.of(toRgbImage(bgra, width, height));
        } catch (IOException | RuntimeException failure) {
            warnThrottled("wgc-sidecar capture failed: hwnd=" + hwnd + " reason=" + failure);
            return Optional.empty();
        }
    }

    private static BufferedImage toRgbImage(byte[] bgra, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[width];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int blue = bgra[index] & 0xFF;
                int green = bgra[index + 1] & 0xFF;
                int red = bgra[index + 2] & 0xFF;
                index += 4;
                row[x] = (red << 16) | (green << 8) | blue;
            }
            image.setRGB(0, y, width, 1, row, 0, width);
        }
        return image;
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder(32);
        int value;
        while ((value = in.read()) >= 0) {
            if (value == '\n') {
                return line.toString();
            }
            line.append((char) value);
            if (line.length() > 200) {
                return null;
            }
        }
        return line.length() == 0 ? null : line.toString();
    }

    private void warnThrottled(String message) {
        long now = System.currentTimeMillis();
        if (now - lastFailureWarnAtMs >= 10_000L) {
            lastFailureWarnAtMs = now;
            log.warn("{} (falling back to LEGACY capture chain)", message);
        }
    }
}
