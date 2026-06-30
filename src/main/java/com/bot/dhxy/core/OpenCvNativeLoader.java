package com.bot.dhxy.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized OpenCV native library initialization.
 */
public final class OpenCvNativeLoader {

    private static final Logger log = LoggerFactory.getLogger(OpenCvNativeLoader.class);
    private static final String WINDOWS_X64_DLL = "opencv_java490.dll";
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private OpenCvNativeLoader() {
    }

    /**
     * Loads the OpenCV native library once for the current JVM.
     *
     * <p>Windows release builds are intended to run with the x64 native DLL only. A local
     * `lib/opencv/windows-x86_64/opencv_java490.dll` copy is preferred so the packaged app does
     * not need to extract every platform native from the full OpenCV dependency jar. The
     * `dhxy.opencv.dll.path` system property or `DHXY_OPENCV_DLL` environment variable can point
     * at another DLL during local verification.</p>
     */
    public static void ensureLoaded() {
        if (LOADED.compareAndSet(false, true)) {
            try {
                loadNativeLibrary();
            } catch (RuntimeException | Error ex) {
                LOADED.set(false);
                throw ex;
            }
        }
    }

    private static void loadNativeLibrary() {
        Path explicitDll = explicitDllPath();
        if (explicitDll != null) {
            System.load(explicitDll.toString());
            log.info("OpenCV native loaded from explicit dll path={}", explicitDll);
            return;
        }

        if (isWindows()) {
            if (!isX64Architecture()) {
                throw new IllegalStateException("DHXY OpenCV runtime is configured for Windows x64 only, arch="
                        + System.getProperty("os.arch", "unknown"));
            }
            Path localDll = Paths.get("lib", "opencv", "windows-x86_64", WINDOWS_X64_DLL)
                    .toAbsolutePath()
                    .normalize();
            if (Files.isRegularFile(localDll)) {
                System.load(localDll.toString());
                log.info("OpenCV native loaded from bundled Windows x64 dll path={}", localDll);
                return;
            }
            log.warn("Windows x64 OpenCV dll not found at {}, falling back to dependency native loader", localDll);
        }

        nu.pattern.OpenCV.loadLocally();
        log.info("OpenCV native loaded through dependency native loader");
    }

    private static Path explicitDllPath() {
        String configuredPath = System.getProperty("dhxy.opencv.dll.path");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("DHXY_OPENCV_DLL");
        }
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        Path path = Paths.get(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Configured OpenCV dll does not exist: " + path);
        }
        return path;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static boolean isX64Architecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64");
    }
}
