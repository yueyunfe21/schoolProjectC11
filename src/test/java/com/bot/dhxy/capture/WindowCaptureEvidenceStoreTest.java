package com.bot.dhxy.capture;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowCaptureEvidenceStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistFreezesPixelsAndReturnsBeforePngWriterRuns() throws Exception {
        ExecutorService writer = Executors.newSingleThreadExecutor();
        CountDownLatch writerOccupied = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        writer.execute(() -> {
            writerOccupied.countDown();
            try {
                releaseWriter.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(writerOccupied.await(2, TimeUnit.SECONDS));

        WindowCaptureEvidenceStore store = new WindowCaptureEvidenceStore(
                new WindowTaskContextHolder(new WindowIsolationProperties()), tempDir, writer);
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(1, 1, Color.RED.getRGB());
        WindowNativeBinding binding = new WindowNativeBinding(
                "1234", "test-window", "test-class", 9L, 10, 20, 3, 2);

        try {
            store.persist(source, binding, "TEST", 10, 20, 13, 22);
            assertFalse(hasExtension(tempDir, ".png"),
                    "PNG writing must stay queued while the dedicated writer is occupied");

            source.setRGB(1, 1, Color.BLUE.getRGB());
            source.flush();
        } finally {
            releaseWriter.countDown();
            store.shutdownExecutors();
        }

        List<Path> pngs = filesWithExtension(tempDir, ".png");
        List<Path> metadata = filesWithExtension(tempDir, ".meta");
        assertEquals(1, pngs.size());
        assertEquals(1, metadata.size());
        BufferedImage saved = ImageIO.read(pngs.get(0).toFile());
        assertEquals(Color.RED.getRGB(), saved.getRGB(1, 1),
                "the queued writer must persist the caller-time pixel snapshot");
        String sidecar = Files.readString(metadata.get(0));
        assertTrue(sidecar.contains("windowId=unbound_1234"));
        assertTrue(sidecar.contains("provider=TEST"));
        assertTrue(sidecar.contains("rect=10,20,13,22"));
    }

    @Test
    void memberCaptureIsNotPersisted() throws Exception {
        ExecutorService writer = Executors.newSingleThreadExecutor();
        WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        WindowCaptureEvidenceStore store = new WindowCaptureEvidenceStore(contextHolder, tempDir, writer);
        WindowRuntimeContext member = new WindowRuntimeContext("member-window", new GameContext());
        member.setRole(WindowRole.MEMBER);
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);

        try {
            contextHolder.runWith(member,
                    () -> store.persist(source, WindowNativeBinding.empty(), "MEMBER_TEST", 0, 0, 3, 2));
        } finally {
            store.shutdownExecutors();
            source.flush();
        }

        assertFalse(hasExtension(tempDir, ".png"));
        assertFalse(hasExtension(tempDir, ".meta"));
    }

    private boolean hasExtension(Path root, String extension) throws Exception {
        return !filesWithExtension(root, extension).isEmpty();
    }

    private List<Path> filesWithExtension(Path root, String extension) throws Exception {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .toList();
        }
    }
}
