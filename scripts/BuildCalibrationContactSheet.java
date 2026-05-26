import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class BuildCalibrationContactSheet {
    public static void main(String[] args) throws Exception {
        Path dir = args.length > 0
                ? Path.of(args[0])
                : Path.of("images", "calibrate", "mapName_coordinate");
        Path out = args.length > 1
                ? Path.of(args[1])
                : Path.of("images", "temp", "map_name_coordinate_contact_sheet.png");

        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().startsWith("Snipaste_"))
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                    .toList();
        }

        int cellW = 360;
        int cellH = 120;
        int cols = 2;
        int rows = (int) Math.ceil(files.size() / (double) cols);
        BufferedImage sheet = new BufferedImage(cellW * cols, cellH * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            int x = col * cellW;
            int y = row * cellH;
            g.setColor(new Color(245, 245, 245));
            g.fillRect(x, y, cellW, cellH);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, cellW - 1, cellH - 1);
            g.drawString(String.format("%02d %s", i + 1, file.getFileName()), x + 6, y + 18);

            int maxW = cellW - 12;
            int maxH = cellH - 28;
            double scale = Math.min(maxW / (double) image.getWidth(), maxH / (double) image.getHeight());
            int drawW = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawH = Math.max(1, (int) Math.round(image.getHeight() * scale));
            g.drawImage(image, x + 6, y + 24, drawW, drawH, null);
            image.flush();
        }
        g.dispose();
        Files.createDirectories(out.getParent());
        ImageIO.write(sheet, "png", out.toFile());
        System.out.println(out.toAbsolutePath());
    }
}
