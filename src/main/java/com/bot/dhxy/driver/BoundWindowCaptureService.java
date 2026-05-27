package com.bot.dhxy.driver;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Per-HWND screenshot provider.
 *
 * It intentionally does not focus or foreground the target window. It asks Win32
 * to render/copy the specific HWND, then crops the requested window-relative area.
 */
@Slf4j
@Service
public class BoundWindowCaptureService {

    private static final int PRINT_WINDOW_RENDER_FULL_CONTENT = 0x00000002;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int DIB_RGB_COLORS = 0;
    private static final int BI_RGB = 0;

    public Optional<CaptureResult> captureRegion(WindowNativeBinding binding,
                                                 int windowBaseX,
                                                 int windowBaseY,
                                                 int x1,
                                                 int y1,
                                                 int x2,
                                                 int y2) {
        if (binding == null || !binding.hasNativeHandle()) {
            return Optional.empty();
        }
        int startX = Math.min(x1, x2);
        int startY = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }

        Optional<CaptureResult> fullWindow = captureWindow(binding);
        if (fullWindow.isEmpty()) {
            return Optional.empty();
        }

        BufferedImage windowImage = fullWindow.get().image();
        try {
            int relativeX = startX - windowBaseX;
            int relativeY = startY - windowBaseY;
            if (relativeX < 0 || relativeY < 0
                    || relativeX + width > windowImage.getWidth()
                    || relativeY + height > windowImage.getHeight()) {
                log.warn("HWND capture crop outside window: hwnd={} title={} base=({}, {}) rect=({}, {})-({}, {}) relative=({}, {}) size={}x{} image={}x{}",
                        binding.getNativeHandle(), binding.getTitle(), windowBaseX, windowBaseY,
                        x1, y1, x2, y2, relativeX, relativeY, width, height,
                        windowImage.getWidth(), windowImage.getHeight());
                return Optional.empty();
            }

            BufferedImage cropped = windowImage.getSubimage(relativeX, relativeY, width, height);
            return Optional.of(new CaptureResult(copiedImage(cropped), fullWindow.get().provider()));
        } finally {
            windowImage.flush();
        }
    }

    public boolean captureRegionToFile(WindowNativeBinding binding,
                                       int windowBaseX,
                                       int windowBaseY,
                                       String savePath,
                                       int x1,
                                       int y1,
                                       int x2,
                                       int y2) {
        Optional<CaptureResult> result = captureRegion(binding, windowBaseX, windowBaseY, x1, y1, x2, y2);
        if (result.isEmpty()) {
            return false;
        }
        try {
            Path path = Path.of(savePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            BufferedImage image = result.get().image();
            try {
                return ImageIO.write(image, "png", path.toFile());
            } finally {
                image.flush();
            }
        } catch (Exception e) {
            log.warn("HWND capture write failed: path={} reason={}", savePath, e.getMessage(), e);
            return false;
        }
    }

    public Optional<CaptureResult> captureWindow(WindowNativeBinding binding) {
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return Optional.empty();
        }
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(hwnd, rect)) {
            return Optional.empty();
        }
        int width = Math.max(0, rect.right - rect.left);
        int height = Math.max(0, rect.bottom - rect.top);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }

        long hwndValue = Pointer.nativeValue(hwnd.getPointer());
        Optional<BufferedImage> printWindow = captureWithCompatibleBitmap(hwnd, width, height,
                (windowDc, memoryDc) -> User32.INSTANCE.PrintWindow(hwnd, memoryDc, PRINT_WINDOW_RENDER_FULL_CONTENT));
        BlankProbe printWindowBlankProbe = probeBlank(printWindow.orElse(null));
        log.info("HWND capture probe: hwnd={} title={} provider=PRINTWINDOW present={} blank={} differentSamples={} firstRgb={} size={}x{}",
                hwndValue, binding.getTitle(), printWindow.isPresent(), printWindowBlankProbe.blank(),
                printWindowBlankProbe.differentSamples(), printWindowBlankProbe.firstRgbHex(), width, height);
        if (printWindow.isPresent() && !printWindowBlankProbe.blank()) {
            return Optional.of(new CaptureResult(printWindow.get(), CaptureProvider.HWND_PRINTWINDOW));
        }

        Optional<BufferedImage> bitBlt = captureWithCompatibleBitmap(hwnd, width, height,
                (windowDc, memoryDc) -> GDI32.INSTANCE.BitBlt(memoryDc, 0, 0, width, height, windowDc, 0, 0, SRCCOPY));
        BlankProbe bitBltBlankProbe = probeBlank(bitBlt.orElse(null));
        log.info("HWND capture probe: hwnd={} title={} provider=BITBLT present={} blank={} differentSamples={} firstRgb={} size={}x{}",
                hwndValue, binding.getTitle(), bitBlt.isPresent(), bitBltBlankProbe.blank(),
                bitBltBlankProbe.differentSamples(), bitBltBlankProbe.firstRgbHex(), width, height);
        if (bitBlt.isPresent() && !bitBltBlankProbe.blank()) {
            printWindow.ifPresent(BufferedImage::flush);
            return Optional.of(new CaptureResult(bitBlt.get(), CaptureProvider.HWND_BITBLT));
        }

        log.warn("HWND capture fallback to possibly blank image: hwnd={} title={} printWindowPresent={} printWindowBlank={} bitBltPresent={} bitBltBlank={}",
                hwndValue, binding.getTitle(), printWindow.isPresent(), printWindowBlankProbe.blank(),
                bitBlt.isPresent(), bitBltBlankProbe.blank());
        return printWindow
                .map(image -> new CaptureResult(image, CaptureProvider.HWND_PRINTWINDOW))
                .or(() -> bitBlt.map(image -> new CaptureResult(image, CaptureProvider.HWND_BITBLT)));
    }

    private Optional<BufferedImage> captureWithCompatibleBitmap(WinDef.HWND hwnd,
                                                               int width,
                                                               int height,
                                                               NativeCapture nativeCapture) {
        WinDef.HDC windowDc = null;
        WinDef.HDC memoryDc = null;
        WinDef.HBITMAP bitmap = null;
        WinNT.HANDLE oldObject = null;
        try {
            windowDc = User32Extra.INSTANCE.GetWindowDC(hwnd);
            if (windowDc == null) {
                return Optional.empty();
            }
            memoryDc = GDI32.INSTANCE.CreateCompatibleDC(windowDc);
            if (memoryDc == null) {
                return Optional.empty();
            }
            bitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDc, width, height);
            if (bitmap == null) {
                return Optional.empty();
            }

            oldObject = GDI32.INSTANCE.SelectObject(memoryDc, bitmap);
            boolean nativeSuccess = nativeCapture.capture(windowDc, memoryDc);
            if (!nativeSuccess) {
                log.debug("HWND native capture returned false: hwnd={}", Pointer.nativeValue(hwnd.getPointer()));
            }
            return Optional.of(bitmapToImage(memoryDc, bitmap, width, height));
        } catch (Exception e) {
            log.debug("HWND native capture failed: hwnd={} reason={}",
                    Pointer.nativeValue(hwnd.getPointer()), e.getMessage());
            return Optional.empty();
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

        Memory buffer = new Memory((long) width * height * 4);
        int lines = GDI32.INSTANCE.GetDIBits(hdc, bitmap, 0, height, buffer, info, DIB_RGB_COLORS);
        if (lines <= 0) {
            throw new IllegalStateException("GetDIBits failed");
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

    private WinDef.HWND toHwnd(WindowNativeBinding binding) {
        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(handle));
    }

    private BufferedImage copiedImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private BlankProbe probeBlank(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return new BlankProbe(true, 0, "null");
        }
        int first = image.getRGB(0, 0) & 0x00ffffff;
        int differentSamples = 0;
        int stepX = Math.max(1, image.getWidth() / 80);
        int stepY = Math.max(1, image.getHeight() / 80);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                if ((image.getRGB(x, y) & 0x00ffffff) != first) {
                    differentSamples++;
                    if (differentSamples >= 8) {
                        return new BlankProbe(false, differentSamples, String.format("#%06X", first));
                    }
                }
            }
        }
        return new BlankProbe(true, differentSamples, String.format("#%06X", first));
    }

    @FunctionalInterface
    private interface NativeCapture {
        boolean capture(WinDef.HDC windowDc, WinDef.HDC memoryDc);
    }

    private interface User32Extra extends StdCallLibrary {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

        WinDef.HDC GetWindowDC(WinDef.HWND hwnd);
    }

    public enum CaptureProvider {
        HWND_PRINTWINDOW,
        HWND_BITBLT
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    public static class CaptureResult {

        BufferedImage image;

        CaptureProvider provider;

    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class BlankProbe {

        boolean blank;

        int differentSamples;

        String firstRgbHex;

    }
}
