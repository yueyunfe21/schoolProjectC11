package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnMapLabelTemplateArguments;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.regex.Pattern;

/** Saves a trusted Cloud-cropped map-label PNG into the Client's durable template directory. */
@Component
public final class MapLabelTemplateLocalOperationExecutor {

    private static final Pattern SAFE_MAP_NAME = Pattern.compile("[\\p{L}\\p{N}_-]{2,32}");
    private static final Path TEMPLATE_ROOT = Path.of("images", "template", "map_label")
            .toAbsolutePath().normalize();

    /**
     * Persist one map-label PNG without replacing an existing user-validated template.
     *
     * @param arguments canonical map name and Base64 PNG bytes; both must be nonblank.
     * @return completed SAVED/EXISTS, or a fail-closed validation/write result.
     */
    public LocalServiceExecution execute(TurnMapLabelTemplateArguments arguments) {
        if (arguments == null || arguments.mapName() == null || arguments.pngBase64() == null) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_ARGUMENTS", null);
        }
        String mapName = arguments.mapName().trim();
        if (!SAFE_MAP_NAME.matcher(mapName).matches()) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_MAP_NAME", null);
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(arguments.pngBase64());
        } catch (IllegalArgumentException invalidBase64) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_BASE64", null);
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException unreadable) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_PNG", null);
        }
        if (image == null) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_PNG", null);
        }
        try {
            if (image.getWidth() < 8 || image.getHeight() < 6
                    || image.getWidth() > 220 || image.getHeight() > 40) {
                return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_INVALID_DIMENSIONS", null);
            }
        } finally {
            image.flush();
        }

        Path target = TEMPLATE_ROOT.resolve(mapName + ".png").normalize();
        if (!target.startsWith(TEMPLATE_ROOT)) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_PATH_REJECTED", null);
        }
        try {
            Files.createDirectories(TEMPLATE_ROOT);
            if (Files.exists(target)) {
                return LocalServiceExecution.completed("MAP_LABEL_TEMPLATE_EXISTS", null, null);
            }
            Path temp = Files.createTempFile(TEMPLATE_ROOT, mapName + "-", ".tmp");
            try {
                Files.write(temp, png);
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temp, target);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            return LocalServiceExecution.completed("MAP_LABEL_TEMPLATE_SAVED", null, null);
        } catch (java.nio.file.FileAlreadyExistsException alreadyExists) {
            return LocalServiceExecution.completed("MAP_LABEL_TEMPLATE_EXISTS", null, null);
        } catch (IOException writeFailure) {
            return LocalServiceExecution.failed("MAP_LABEL_TEMPLATE_WRITE_FAILED", null);
        }
    }
}
