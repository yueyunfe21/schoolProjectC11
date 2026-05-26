package com.bot.dhxy.window.diagnostics;

import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 调试用：验证 Win32 per-HWND 截图路径是否能拿到游戏窗口内容。
 *
 * 这里刻意不做 focus、不走 Robot、不发任何输入，只比较：
 * 1. PrintWindow：让目标窗口自己渲染到内存 DC；
 * 2. BitBlt：从目标窗口 DC 复制像素。
 */
@Slf4j
@Service
public class WindowCaptureExperimentService {

    private static final int PRINT_WINDOW_RENDER_FULL_CONTENT = 0x00000002;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int DIB_RGB_COLORS = 0;
    private static final int BI_RGB = 0;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path outputDir = Path.of("images", "temp", "window_capture_experiment")
            .toAbsolutePath()
            .normalize();

    public List<WindowCaptureExperimentResult> captureSelectedWindows(List<WindowTaskSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }

        List<WindowCaptureExperimentResult> results = new ArrayList<>();
        for (WindowTaskSnapshot snapshot : snapshots) {
            results.add(captureOne(snapshot));
        }
        return results;
    }

    private WindowCaptureExperimentResult captureOne(WindowTaskSnapshot snapshot) {
        String windowId = snapshot == null ? null : snapshot.getWindowId();
        WindowNativeBinding binding = snapshot == null ? null : snapshot.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return WindowCaptureExperimentResult.failed(windowId, "窗口没有 native handle，先注册/刷新窗口");
        }

        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle <= 0) {
            return WindowCaptureExperimentResult.failed(windowId, "native handle 无法解析：" + binding.getNativeHandle());
        }

        WinDef.HWND hwnd = new WinDef.HWND(new Pointer(handle));
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(hwnd, rect)) {
            return WindowCaptureExperimentResult.failed(windowId, "GetWindowRect 失败：" + binding.getNativeHandle());
        }

        int width = Math.max(0, rect.right - rect.left);
        int height = Math.max(0, rect.bottom - rect.top);
        if (width <= 0 || height <= 0) {
            return WindowCaptureExperimentResult.failed(windowId, "窗口尺寸无效：" + width + "x" + height);
        }

        try {
            Files.createDirectories(outputDir);
            String stamp = FILE_TIME_FORMATTER.format(LocalDateTime.now());
            String prefix = sanitizeFileName(windowId == null ? "window" : windowId) + "_" + stamp;

            CaptureOutput printWindow = captureByPrintWindow(hwnd, width, height,
                    outputDir.resolve(prefix + "_printwindow.png"));
            CaptureOutput bitBlt = captureByBitBlt(hwnd, width, height,
                    outputDir.resolve(prefix + "_bitblt.png"));

            WindowCaptureExperimentResult result = new WindowCaptureExperimentResult(
                    windowId,
                    binding.getNativeHandle(),
                    binding.getTitle(),
                    width,
                    height,
                    printWindow,
                    bitBlt,
                    null
            );
            log.info("[窗口截图实验] windowId={} hwnd={} title={} size={}x{} printWindow={} bitBlt={}",
                    windowId, binding.getNativeHandle(), binding.getTitle(), width, height,
                    printWindow.toLogText(), bitBlt.toLogText());
            return result;
        } catch (Exception e) {
            log.warn("[窗口截图实验] 执行失败 windowId={} hwnd={}: {}", windowId, binding.getNativeHandle(), e.getMessage(), e);
            return WindowCaptureExperimentResult.failed(windowId, "截图实验异常：" + e.getMessage());
        }
    }

    private CaptureOutput captureByPrintWindow(WinDef.HWND hwnd, int width, int height, Path outputPath) {
        return captureWithCompatibleBitmap(hwnd, width, height, outputPath,
                (windowDc, memoryDc) -> User32.INSTANCE.PrintWindow(hwnd, memoryDc, PRINT_WINDOW_RENDER_FULL_CONTENT));
    }

    private CaptureOutput captureByBitBlt(WinDef.HWND hwnd, int width, int height, Path outputPath) {
        return captureWithCompatibleBitmap(hwnd, width, height, outputPath,
                (windowDc, memoryDc) -> GDI32.INSTANCE.BitBlt(memoryDc, 0, 0, width, height, windowDc, 0, 0, SRCCOPY));
    }

    private CaptureOutput captureWithCompatibleBitmap(WinDef.HWND hwnd,
                                                      int width,
                                                      int height,
                                                      Path outputPath,
                                                      NativeCapture nativeCapture) {
        WinDef.HDC windowDc = null;
        WinDef.HDC memoryDc = null;
        WinDef.HBITMAP bitmap = null;
        WinNT.HANDLE oldObject = null;
        try {
            windowDc = User32Extra.INSTANCE.GetWindowDC(hwnd);
            if (windowDc == null) {
                return CaptureOutput.failed(outputPath, "GetWindowDC 返回空");
            }
            memoryDc = GDI32.INSTANCE.CreateCompatibleDC(windowDc);
            if (memoryDc == null) {
                return CaptureOutput.failed(outputPath, "CreateCompatibleDC 返回空");
            }
            bitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDc, width, height);
            if (bitmap == null) {
                return CaptureOutput.failed(outputPath, "CreateCompatibleBitmap 返回空");
            }

            oldObject = GDI32.INSTANCE.SelectObject(memoryDc, bitmap);
            boolean nativeSuccess = nativeCapture.capture(windowDc, memoryDc);
            BufferedImage image = bitmapToImage(memoryDc, bitmap, width, height);
            ImageIO.write(image, "png", outputPath.toFile());
            return new CaptureOutput(nativeSuccess, true, isProbablyBlank(image), outputPath, null);
        } catch (Exception e) {
            return CaptureOutput.failed(outputPath, e.getMessage());
        } finally {
            if (memoryDc != null && oldObject != null) {
                GDI32.INSTANCE.SelectObject(memoryDc, oldObject);
            }
            if (bitmap != null) {
                GDI32.INSTANCE.DeleteObject(bitmap);
            }
            if (memoryDc != null) {
                GDI32.INSTANCE.DeleteDC(memoryDc);
            }
            if (windowDc != null) {
                User32.INSTANCE.ReleaseDC(hwnd, windowDc);
            }
        }
    }

    private BufferedImage bitmapToImage(WinDef.HDC hdc, WinDef.HBITMAP bitmap, int width, int height) {
        WinGDI.BITMAPINFO info = new WinGDI.BITMAPINFO();
        info.bmiHeader.biWidth = width;
        info.bmiHeader.biHeight = -height;
        info.bmiHeader.biPlanes = 1;
        info.bmiHeader.biBitCount = 32;
        info.bmiHeader.biCompression = BI_RGB;

        int byteCount = width * height * 4;
        Memory buffer = new Memory(byteCount);
        int lines = GDI32.INSTANCE.GetDIBits(hdc, bitmap, 0, height, buffer, info, DIB_RGB_COLORS);
        if (lines <= 0) {
            throw new IllegalStateException("GetDIBits 失败");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int offset = i * 4;
            int b = buffer.getByte(offset) & 0xff;
            int g = buffer.getByte(offset + 1) & 0xff;
            int r = buffer.getByte(offset + 2) & 0xff;
            pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private boolean isProbablyBlank(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return true;
        }
        int first = image.getRGB(0, 0) & 0x00ffffff;
        int differentSamples = 0;
        int totalSamples = 0;
        int stepX = Math.max(1, image.getWidth() / 80);
        int stepY = Math.max(1, image.getHeight() / 80);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                totalSamples++;
                if ((image.getRGB(x, y) & 0x00ffffff) != first) {
                    differentSamples++;
                    if (differentSamples >= 8) {
                        return false;
                    }
                }
            }
        }
        return totalSamples > 0;
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "window";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @FunctionalInterface
    private interface NativeCapture {
        boolean capture(WinDef.HDC windowDc, WinDef.HDC memoryDc);
    }

    private interface User32Extra extends StdCallLibrary {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

        WinDef.HDC GetWindowDC(WinDef.HWND hwnd);
    }

    public static class WindowCaptureExperimentResult {
        private final String windowId;
        private final String nativeHandle;
        private final String title;
        private final int width;
        private final int height;
        private final CaptureOutput printWindow;
        private final CaptureOutput bitBlt;
        private final String errorMessage;

        private WindowCaptureExperimentResult(String windowId,
                                              String nativeHandle,
                                              String title,
                                              int width,
                                              int height,
                                              CaptureOutput printWindow,
                                              CaptureOutput bitBlt,
                                              String errorMessage) {
            this.windowId = windowId;
            this.nativeHandle = nativeHandle;
            this.title = title;
            this.width = width;
            this.height = height;
            this.printWindow = printWindow;
            this.bitBlt = bitBlt;
            this.errorMessage = errorMessage;
        }

        private static WindowCaptureExperimentResult failed(String windowId, String message) {
            return new WindowCaptureExperimentResult(windowId, null, null, 0, 0, null, null, message);
        }

        public String getWindowId() { return windowId; }

        public boolean isSuccess() {
            return errorMessage == null
                    && ((printWindow != null && printWindow.imageWritten)
                    || (bitBlt != null && bitBlt.imageWritten));
        }

        public String toDetailMessage() {
            if (errorMessage != null) {
                return errorMessage;
            }
            return "hwnd=" + nativeHandle
                    + " size=" + width + "x" + height
                    + " PrintWindow=" + outputSummary(printWindow)
                    + " BitBlt=" + outputSummary(bitBlt)
                    + " title=" + (title == null || title.isBlank() ? "-" : title);
        }

        private String outputSummary(CaptureOutput output) {
            if (output == null) {
                return "未执行";
            }
            if (output.errorMessage != null) {
                return "失败(" + output.errorMessage + ")";
            }
            return (output.nativeSuccess ? "nativeOK" : "nativeFalse")
                    + ", written=" + output.imageWritten
                    + ", blank=" + output.probablyBlank
                    + ", path=" + output.outputPath;
        }
    }

    public static class CaptureOutput {
        private final boolean nativeSuccess;
        private final boolean imageWritten;
        private final boolean probablyBlank;
        private final Path outputPath;
        private final String errorMessage;

        private CaptureOutput(boolean nativeSuccess,
                              boolean imageWritten,
                              boolean probablyBlank,
                              Path outputPath,
                              String errorMessage) {
            this.nativeSuccess = nativeSuccess;
            this.imageWritten = imageWritten;
            this.probablyBlank = probablyBlank;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
        }

        private static CaptureOutput failed(Path outputPath, String message) {
            return new CaptureOutput(false, false, true, outputPath, message);
        }

        private String toLogText() {
            if (errorMessage != null) {
                return "failed(" + errorMessage + ")";
            }
            return "nativeSuccess=" + nativeSuccess
                    + ", imageWritten=" + imageWritten
                    + ", probablyBlank=" + probablyBlank
                    + ", path=" + outputPath;
        }
    }
}
