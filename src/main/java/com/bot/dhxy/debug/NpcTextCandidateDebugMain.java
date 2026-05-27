package com.bot.dhxy.debug;

import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.model.ocr.TextCandidate;
import com.bot.dhxy.model.ocr.TextCandidateScanResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Standalone debug entry for shape-based NPC-name candidate detection on washed images.
 *
 * <p>Usage: pass a black/white washed PNG path as the first argument and optionally an overlay PNG
 * path as the second argument. The main does not start Spring, capture windows, OCR, focus, or click;
 * it only reads the supplied image and prints candidate rectangles/scores.</p>
 */
public class NpcTextCandidateDebugMain {

    /**
     * Run candidate detection on a washed image file.
     *
     * @param args {@code args[0]} is the washed image path. {@code args[1]} is an optional overlay
     * output path. When omitted, the overlay is written beside the input as
     * {@code <input-stem>_candidates.png}.
     * @throws Exception when image reading or overlay writing fails.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            System.err.println("Usage: NpcTextCandidateDebugMain <washed-png> [overlay-png]");
            return;
        }

        Path inputPath = Path.of(args[0]).toAbsolutePath().normalize();
        Path overlayPath = args.length >= 2 && args[1] != null && !args[1].isBlank()
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : defaultOverlayPath(inputPath);

        BufferedImage image = ImageIO.read(inputPath.toFile());
        if (image == null) {
            System.err.println("Unreadable image: " + inputPath);
            return;
        }
        try {
            GameTextLineOcrService service = new GameTextLineOcrService(null);
            TextCandidateScanResult result =
                    service.findTextLikeCandidateResultFromWashedImage(image, overlayPath);
            List<TextCandidate> candidates = result.candidates();
            System.out.println("input=" + inputPath);
            System.out.println("overlay=" + overlayPath);
            System.out.println("status=" + result.status());
            System.out.println("message=" + result.message());
            System.out.println("candidateCount=" + candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                System.out.println((i + 1) + ". " + candidates.get(i).toSummaryText());
            }
        } finally {
            image.flush();
        }
    }

    private static Path defaultOverlayPath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        Path parent = inputPath.getParent();
        Path output = Path.of(stem + "_candidates.png");
        return parent == null ? output : parent.resolve(output);
    }
}
