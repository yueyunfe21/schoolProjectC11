package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Replays the exact G065 White Bone Mountain frame where an open map hides the 看打 dialog. */
class G065GhostKingMapBlockerReplayTest {

    private static final Path CASE_DIR = Path.of("images/test-cases/guiwang/g065");

    public static void main(String[] args) throws Exception {
        new G065GhostKingMapBlockerReplayTest()
                .openMapIsRecognizedAndKandaFailureIsDeferredUntilTheNextFrame();
        System.out.println("G065 Ghost King map-blocker replay passed");
    }

    @Test
    void openMapIsRecognizedAndKandaFailureIsDeferredUntilTheNextFrame() throws Exception {
        Path rawPath = CASE_DIR.resolve("white-bone-map-blocker-raw.png");
        Path mapTemplatePath = Path.of("images/template/map/checkbox_unchecked.png");
        Path kandaTemplatePath = Path.of("images/template/dialog/guiwang/jinzhan.png");
        BufferedImage raw = ImageIO.read(rawPath.toFile());
        BufferedImage mapTemplate = ImageIO.read(mapTemplatePath.toFile());
        BufferedImage kandaTemplate = ImageIO.read(kandaTemplatePath.toFile());
        assertNotNull(raw);
        assertNotNull(mapTemplate);
        assertNotNull(kandaTemplate);

        double[] mapHit = ImageFinder.find(raw, mapTemplate, 0.95);
        assertNotNull(mapHit, "the exact failed frame must prove that the map overlay is open");
        assertNull(ImageFinder.find(raw, kandaTemplate, 0.8),
                "the covered frame must not fabricate a 看打 option");

        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(raw, 0, 0, null);
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(3.0f));
        int left = (int) Math.round(mapHit[0] - mapTemplate.getWidth() / 2.0);
        int top = (int) Math.round(mapHit[1] - mapTemplate.getHeight() / 2.0);
        graphics.drawRect(left, top, mapTemplate.getWidth(), mapTemplate.getHeight());
        graphics.drawString("MAP BLOCKER score=" + String.format("%.4f", mapHit[2]),
                Math.max(5, left), Math.max(18, top - 5));
        graphics.dispose();
        Path markedPath = CASE_DIR.resolve("white-bone-map-blocker-marked.png");
        ImageIO.write(marked, "png", markedPath.toFile());

        String sampler = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java"));
        int probe = sampler.indexOf("cleaner.probeMapWindowPresent(");
        int cleanup = sampler.indexOf("cleaner.closeMapIfPresent(", probe);
        int failure = sampler.indexOf("context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule)", cleanup);
        assertTrue(probe >= 0 && cleanup > probe && failure > cleanup,
                "map detection and narrow cleanup must run before the one-shot missing-kanda failure claim");

        marked.flush();
        raw.flush();
        mapTemplate.flush();
        kandaTemplate.flush();
    }
}
