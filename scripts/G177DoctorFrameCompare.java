import com.bot.dhxy.core.ImageFinder;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** User-authorized G177 image comparison; never sends game input. --verify requires lossless positives. */
public class G177DoctorFrameCompare {
    public static void main(String[] args) throws Exception {
        boolean verify = "--verify".equals(args[0]);
        int templateIndex = verify ? 1 : 0;
        BufferedImage template = ImageIO.read(Path.of(args[templateIndex]).toFile());
        if (template == null) throw new IllegalArgumentException("Missing template");
        for (int i = templateIndex + 1; i < args.length; i++) {
            BufferedImage frame = ImageIO.read(Path.of(args[i]).toFile());
            if (frame == null) throw new IllegalArgumentException("Missing frame: " + args[i]);
            // Same fixed raw ROI as Cloud DialogService, in window-relative image pixels.
            BufferedImage strip = frame.getSubimage(260, 373, 118, 40);
            double[] fixed = ImageFinder.find(strip, template, -1.0);
            // Diagnostic comparison only: existing full dialog search area, not a production change.
            BufferedImage dialog = frame.getSubimage(200, 250, 640, 300);
            double[] full = ImageFinder.find(dialog, template, -1.0);
            BufferedImage expanded = frame.getSubimage(260, 360, 118, 53);
            double[] proposed = ImageFinder.find(expanded, template, -1.0);
            System.out.printf("%s fixedScore=%.6f fixedPass=%s fullScore=%.6f fullPass=%s templateTopLeft=(%.1f,%.1f) templateSize=%dx%d%n",
                    Path.of(args[i]).getFileName(), fixed[2], fixed[2] >= 0.85,
                    full[2], full[2] >= 0.85,
                    200 + full[0] - template.getWidth() / 2.0,
                    250 + full[1] - template.getHeight() / 2.0,
                    template.getWidth(), template.getHeight());
            System.out.printf("proposedScore=%.6f pass=%s center=(%.1f,%.1f)%n",
                    proposed[2], proposed[2] >= .85, 260 + proposed[0], 360 + proposed[1]);
            if (verify) {
                if (!args[i].endsWith(".png") || proposed[2] < .85) {
                    throw new AssertionError("Lossless positive replay failed: " + args[i]);
                }
                // Factual algorithm overlay; original frame stays untouched. No generated imagery.
                BufferedImage marked = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = marked.createGraphics();
                graphics.drawImage(frame, 0, 0, null);
                graphics.setStroke(new java.awt.BasicStroke(2));
                graphics.setColor(java.awt.Color.ORANGE);
                graphics.drawRect(260, 373, 118, 40);
                graphics.setColor(java.awt.Color.CYAN);
                graphics.drawRect(260, 360, 118, 53);
                int x = 260 + (int) Math.round(proposed[0]);
                int y = 360 + (int) Math.round(proposed[1]);
                graphics.setColor(java.awt.Color.GREEN);
                graphics.drawRect(x-template.getWidth()/2, y-template.getHeight()/2,
                        template.getWidth(), template.getHeight());
                graphics.setColor(java.awt.Color.RED);
                graphics.drawLine(x-5,y,x+5,y);
                graphics.drawLine(x,y-5,x,y+5);
                graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
                graphics.drawString(String.format("MATCH %.6f CENTER (%d,%d) - replay, no input", proposed[2],x,y),260,330);
                graphics.dispose();
                Path output = Path.of(args[i].replace(".png", "_roi_review.png"));
                ImageIO.write(marked, "png", output.toFile());
                System.out.println("Overlay " + output);
            }
            strip.flush();
            dialog.flush();
            frame.flush();
        }
        template.flush();
    }
}
