package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Production-boundary behavior contract for G017's exact shared-frame Client producer. */
class G017UnknownPhaseConnectivityContractTest {

    private static final Path CORPUS = Path.of(System.getProperty(
            "dhxy.dialog.frame.corpus",
            "D:/mavenProject/DHXY-cr271/images/test-cases/dialog-frame-classification"));

    @Test
    void realNoDialogFrameProducesNoneWithoutUploadingSemanticPixels() throws Exception {
        UnknownPhasePresenceLocalMechanics.Sample sample = sample(
                CORPUS.resolve("negative/N05_live_20260728_224456_no_dialog_green_names.png"));

        assertEquals("none", sample.dialogPresence());
        assertNull(sample.dialogPng());
    }

    @Test
    void realOptionFrameStaysUnknownLocallyAndCarriesExactCloudClassificationPixels() throws Exception {
        assertPresentFrameIsFailClosed(
                CORPUS.resolve("positive/P09_live_20260728_231800_option_dialog.png"));
    }

    @Test
    void realStoryFrameStaysUnknownLocallyAndCarriesExactCloudClassificationPixels() throws Exception {
        assertPresentFrameIsFailClosed(
                CORPUS.resolve("positive/P08_live_20260728_224529_story_dialog.png"));
    }

    @Test
    void unavailableSharedFrameProducesUnknownWithoutFabricatingNoneOrOption() {
        BufferedImage title = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        UnknownPhasePresenceLocalMechanics mechanics =
                new UnknownPhasePresenceLocalMechanics(title);
        mechanics.bindCycleFrameCropper(rect -> null);
        try {
            UnknownPhasePresenceLocalMechanics.Sample sample = mechanics.sample();
            assertEquals("unknown", sample.titlePresence());
            assertEquals("unknown", sample.dialogPresence());
            assertNull(sample.dialogPng());
        } finally {
            mechanics.reset();
        }
    }

    private static void assertPresentFrameIsFailClosed(Path source) throws Exception {
        UnknownPhasePresenceLocalMechanics.Sample sample = sample(source);
        assertEquals("unknown", sample.dialogPresence(), source.toString());
        assertNotNull(sample.dialogPng(), source.toString());
        BufferedImage uploaded = ImageIO.read(new ByteArrayInputStream(sample.dialogPng()));
        try {
            assertNotNull(uploaded, source.toString());
            assertEquals(640, uploaded.getWidth());
            assertEquals(300, uploaded.getHeight());
        } finally {
            if (uploaded != null) {
                uploaded.flush();
            }
        }
    }

    private static UnknownPhasePresenceLocalMechanics.Sample sample(Path source) throws Exception {
        BufferedImage full = ImageIO.read(source.toFile());
        assertNotNull(full, source.toString());
        BufferedImage title = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        UnknownPhasePresenceLocalMechanics mechanics =
                new UnknownPhasePresenceLocalMechanics(title);
        mechanics.bindCycleFrameCropper(rect -> full.getSubimage(
                rect[0], rect[1], rect[2], rect[3]));
        try {
            return mechanics.sample();
        } finally {
            full.flush();
            mechanics.reset();
        }
    }
}
