package com.bot.dhxy.vision;

import com.bot.dhxy.model.ocr.OcrWordResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SheyaoxiangDigitTemplateReaderTest {
    public static void main(String[] args) throws Exception {
        recognizesFortySevenWhenDigitTemplatesExist();
        learnsOnlyTheOcrCoveredDigitWhenOcrSeesPartialNumber();
    }

    private static void recognizesFortySevenWhenDigitTemplatesExist() throws Exception {
        Path tempDir = Files.createTempDirectory("sheyaoxiang-digit-templates-test");
        BufferedImage sample = readSample();
        BufferedImage four = sample.getSubimage(12, 144, 30, 54);
        BufferedImage seven = sample.getSubimage(54, 144, 30, 54);
        ImageIO.write(four, "png", tempDir.resolve("4.png").toFile());
        ImageIO.write(seven, "png", tempDir.resolve("7.png").toFile());

        SheyaoxiangDigitTemplateReader reader = new SheyaoxiangDigitTemplateReader(tempDir);
        SheyaoxiangDigitTemplateReader.Result result = reader.recognizeAndLearn(sample, List.of(), "test-47");

        assertEquals("47", result.text(), "template reader should read the full two-digit incense minutes");
        assertEquals(0, result.learnedSymbols().size(), "existing templates should not learn again");
    }

    private static void learnsOnlyTheOcrCoveredDigitWhenOcrSeesPartialNumber() throws Exception {
        Path tempDir = Files.createTempDirectory("sheyaoxiang-digit-templates-test");
        BufferedImage sample = readSample();
        OcrWordResult ocrSeven = new OcrWordResult("7", 61, 168, 34, 134, 55, 69, 0.99984);

        SheyaoxiangDigitTemplateReader reader = new SheyaoxiangDigitTemplateReader(tempDir);
        SheyaoxiangDigitTemplateReader.Result result = reader.recognizeAndLearn(sample, List.of(ocrSeven), "test-47");

        assertEquals("", result.text(), "partial OCR must not be treated as the full remaining minutes");
        assertEquals(List.of("7"), result.learnedSymbols(), "only the OCR-covered right digit should be learned");
        assertTrue(Files.exists(tempDir.resolve("7.png")), "digit 7 template should be created");
        assertTrue(!Files.exists(tempDir.resolve("4.png")), "digit 4 must not be learned from a right-side OCR box");
    }

    private static BufferedImage readSample() throws Exception {
        Path path = Path.of("images", "temp", "hwnd-3F50A4E", "sheyaoxiang_status_green_digits.png");
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new AssertionError("sample image is unreadable: " + path);
        }
        return image;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
