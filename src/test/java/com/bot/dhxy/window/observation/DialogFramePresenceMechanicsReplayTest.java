package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Replays the real dialog-frame corpus used to guard local tracker-link retry suppression. */
class DialogFramePresenceMechanicsReplayTest {

    private static final int DIALOG_X = 200;
    private static final int DIALOG_Y = 250;
    private static final int DIALOG_W = 640;
    private static final int DIALOG_H = 300;
    private static final Path CORPUS_ROOT = Path.of(System.getProperty(
            "dhxy.dialog.frame.corpus",
            "D:/mavenProject/DHXY-cr271/images/test-cases/dialog-frame-classification"));

    @Test
    void realWindowFramesGateLocalTrackerRetriesWithoutReadingDialogText() throws Exception {
        assertTrue(Files.isDirectory(CORPUS_ROOT), "missing dialog-frame corpus: " + CORPUS_ROOT);
        List<Path> cases;
        try (Stream<Path> stream = Files.walk(CORPUS_ROOT, 2)) {
            cases = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".png"))
                    .filter(this::isRawWindowFrame)
                    .filter(path -> {
                        String parent = path.getParent().getFileName().toString();
                        return parent.equals("positive") || parent.equals("negative");
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        assertTrue(!cases.isEmpty(), "dialog-frame corpus is empty");

        DialogFramePresenceMechanics mechanics = new DialogFramePresenceMechanics();
        for (Path source : cases) {
            boolean expected = source.getParent().getFileName().toString().equals("positive");
            BufferedImage fullFrame = ImageIO.read(source.toFile());
            BufferedImage roi = fullFrame.getSubimage(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H);
            try {
                assertEquals(expected, mechanics.isPresent(roi), source.getFileName().toString());
            } finally {
                fullFrame.flush();
            }
        }
    }

    private boolean isRawWindowFrame(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return false;
            }
            try {
                return image.getWidth() >= DIALOG_X + DIALOG_W && image.getHeight() >= DIALOG_Y + DIALOG_H;
            } finally {
                image.flush();
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
