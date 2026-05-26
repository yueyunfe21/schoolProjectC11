import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BuildXiuluoTemplates {
    private static final Path SOURCE_DIR = Path.of("images", "template", "xiuluo");
    private static final Path COMMON_SOURCE_DIR = Path.of("images", "template_sources", "common");
    private static final Path DIALOG_OUT_DIR = Path.of("images", "template", "dialog");
    private static final Path ITEM_OUT_DIR = Path.of("images", "template", "item");
    private static final Path NPC_OUT_DIR = Path.of("images", "template", "npc");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIALOG_OUT_DIR);
        Files.createDirectories(ITEM_OUT_DIR);
        Files.createDirectories(NPC_OUT_DIR);

        exportGreenBandTemplate("accept_dialog.png", 0, "xiuluo_accept_xianlaiwu.png", 42);
        exportGreenBandTemplate("accept_dialog.png", 1, "xiuluo_cancel_task.png", 0);
        exportGreenBandTemplate("under_five_dialog.png", 0, "xiuluo_underfive_confirm.png", 0);
        exportGreenBandTemplate("under_five_dialog.png", 1, "xiuluo_underfive_wait.png", 0);
        exportGreenBandTemplate("enter_battle_dialog.png", 0, "xiuluo_enter_battle_kanda.png", 0);

        Path returnItem = SOURCE_DIR.resolve("return_item.png");
        if (Files.exists(returnItem)) {
            Path out = ITEM_OUT_DIR.resolve("xiuluo_return_item.png");
            Files.copy(returnItem, out, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[ok] copied " + out);
        } else {
            System.out.println("[skip] missing source: " + returnItem);
        }

        exportFixedCrop(COMMON_SOURCE_DIR.resolve("npc_menu_clean_sample.png"),
                NPC_OUT_DIR.resolve("npc_tag.png"), 80, 75, 39, 12);
    }

    private static void exportGreenBandTemplate(String sourceName, int bandIndex, String outName, int leftWidth)
            throws IOException {
        Path source = SOURCE_DIR.resolve(sourceName);
        if (!Files.exists(source)) {
            System.out.println("[skip] missing source: " + source);
            return;
        }
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            System.out.println("[skip] unreadable source: " + source);
            return;
        }
        List<Band> bands = findGreenBands(image);
        if (bands.size() <= bandIndex) {
            System.out.printf("[skip] source=%s expected band %d but found %d%n", sourceName, bandIndex, bands.size());
            return;
        }
        Band band = bands.get(bandIndex);
        Path out = DIALOG_OUT_DIR.resolve(outName);
        writeBinaryGreenCrop(image, band, out, leftWidth);
        System.out.printf("[ok] wrote %s crop=(%d,%d)-(%d,%d)%n",
                out, band.minX(), band.minY(), band.maxX(), band.maxY());
    }

    private static List<Band> findGreenBands(BufferedImage image) {
        int[] rowCounts = new int[image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            int count = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                if (isOptionGreen(image.getRGB(x, y))) {
                    count++;
                }
            }
            rowCounts[y] = count;
        }

        List<Band> bands = new ArrayList<>();
        int startY = -1;
        int endY = -1;
        int gap = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if (rowCounts[y] >= 3) {
                if (startY < 0) {
                    startY = y;
                }
                endY = y;
                gap = 0;
            } else if (startY >= 0) {
                gap++;
                if (gap > 2) {
                    addBandIfUseful(image, startY, endY, bands);
                    startY = -1;
                    endY = -1;
                    gap = 0;
                }
            }
        }
        if (startY >= 0) {
            addBandIfUseful(image, startY, endY, bands);
        }
        bands.sort(Comparator.comparingInt(Band::minY));
        return bands;
    }

    private static void addBandIfUseful(BufferedImage image, int startY, int endY, List<Band> bands) {
        int minX = image.getWidth();
        int maxX = -1;
        int pixels = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isOptionGreen(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    pixels++;
                }
            }
        }
        if (pixels >= 20 && maxX >= minX) {
            bands.add(new Band(Math.max(0, minX - 2), Math.min(image.getWidth() - 1, maxX + 2),
                    Math.max(0, startY - 2), Math.min(image.getHeight() - 1, endY + 2), pixels));
        }
    }

    private static void writeBinaryGreenCrop(BufferedImage source, Band band, Path outPath, int leftWidth)
            throws IOException {
        int minX = band.minX();
        int maxX = band.maxX();
        if (leftWidth > 0) {
            maxX = Math.min(maxX, minX + leftWidth - 1);
        }
        int width = maxX - minX + 1;
        int height = band.maxY() - band.minY() + 1;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean green = isOptionGreen(source.getRGB(minX + x, band.minY() + y));
                out.setRGB(x, y, green ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        Files.createDirectories(outPath.getParent());
        ImageIO.write(out, "png", outPath.toFile());
    }

    private static void exportFixedCrop(Path source, Path outPath, int x, int y, int width, int height)
            throws IOException {
        if (!Files.exists(source)) {
            System.out.println("[skip] missing source: " + source);
            return;
        }
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            System.out.println("[skip] unreadable source: " + source);
            return;
        }
        if (x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IOException("Invalid crop for " + source);
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int yy = 0; yy < height; yy++) {
            for (int xx = 0; xx < width; xx++) {
                out.setRGB(xx, yy, image.getRGB(x + xx, y + yy));
            }
        }
        Files.createDirectories(outPath.getParent());
        ImageIO.write(out, "png", outPath.toFile());
        System.out.printf("[ok] wrote %s crop=(%d,%d)-(%d,%d)%n",
                outPath, x, y, x + width - 1, y + height - 1);
    }

    private static boolean isOptionGreen(int rgb) {
        Color c = new Color(rgb);
        return c.getGreen() > 80 && (c.getGreen() - c.getRed()) > 40 && (c.getGreen() - c.getBlue()) > 40;
    }

    private record Band(int minX, int maxX, int minY, int maxY, int pixels) {
    }
}
