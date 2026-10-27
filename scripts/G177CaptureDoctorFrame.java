import com.bot.dhxy.config.CaptureBackendProperties;
import com.bot.dhxy.driver.WgcSidecarCaptureClient;
import javax.imageio.ImageIO;
import java.nio.file.Path;

/** Authorized diagnostic capture using the application's existing backend; no focus or input. */
public class G177CaptureDoctorFrame {
    public static void main(String[] args) throws Exception {
        var capture = new WgcSidecarCaptureClient(new CaptureBackendProperties());
        var image = capture.captureWindow(Long.parseLong(args[0]), 1036, 783)
                .orElseThrow(() -> new IllegalStateException("No lossless bound-window frame"));
        var output = Path.of(args[1]);
        if (java.nio.file.Files.exists(output)) throw new IllegalStateException("Refuse overwrite " + output);
        if (!ImageIO.write(image, "png", output.toFile())) throw new IllegalStateException("PNG writer unavailable");
        System.out.println("Captured " + java.time.OffsetDateTime.now() + " hwnd=" + args[0] + " 1036x783 " + output);
    }
}
