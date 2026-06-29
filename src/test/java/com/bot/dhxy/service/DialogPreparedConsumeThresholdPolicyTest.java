package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for 修罗 prepared enter-battle consume validation.
 *
 * <p>The 2026-06-23 round-23 failure repeatedly prepared the visible `看打！` option but rejected
 * every consume attempt at distance=11 while the global threshold was 8. Live validation needs a
 * game-window screenshot, so this test protects the narrow policy shape: only 修罗 enter-battle gets
 * a slightly wider green-template tolerance, while other prepared dialog actions keep the stricter
 * default.</p>
 */
public class DialogPreparedConsumeThresholdPolicyTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String dialogService = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/DialogService.java"), StandardCharsets.UTF_8);
        String normalized = dialogService.replace("\r\n", "\n");

        require(dialogService.contains("PREPARED_DIALOG_FINGERPRINT_MAX_DISTANCE = 8"),
                "default prepared-dialog fingerprint threshold must remain strict at 8");
        require(dialogService.contains("XIULUO_ENTER_BATTLE_PREPARED_FINGERPRINT_MAX_DISTANCE = 16"),
                "Xiuluo enter-battle must have its own narrow threshold for the observed distance=11 drift");

        String validator = between(dialogService,
                "public PreparedDialogAction validatePreparedDialogActionForConsume(",
                "private BufferedImage washPreparedValidationCrop(");
        require(validator.contains("int maxDistance = preparedDialogFingerprintMaxDistance(action);"),
                "consume validation must resolve the threshold from the prepared action operation");
        require(validator.contains("distance <= maxDistance"),
                "consume validation must compare against the operation-scoped threshold");
        require(!validator.contains("distance <= PREPARED_DIALOG_FINGERPRINT_MAX_DISTANCE"),
                "consume validation must not use the global threshold directly");

        String policy = between(dialogService,
                "private int preparedDialogFingerprintMaxDistance(",
                "private BufferedImage washPreparedValidationCrop(");
        require(policy.contains("DialogOperation.XIULUO_ENTER_BATTLE"),
                "threshold policy must explicitly scope the wider tolerance to Xiuluo enter-battle");
        require(policy.contains("return XIULUO_ENTER_BATTLE_PREPARED_FINGERPRINT_MAX_DISTANCE"),
                "Xiuluo enter-battle must return the dedicated threshold");
        require(policy.contains("return PREPARED_DIALOG_FINGERPRINT_MAX_DISTANCE"),
                "all other prepared actions must keep the default threshold");

        require(normalized.contains(".build(),\n"
                        + "                        \"template-specific\",\n"
                        + "                        washedPath,\n"
                        + "                        true);"),
                "green-template prepared actions must consume with the same template-specific wash used during prepare");
        require(!normalized.contains(".build(),\n"
                        + "                        \"green\",\n"
                        + "                        washedPath,\n"
                        + "                        true);"),
                "green-template prepared actions must not switch to plain green wash at consume time");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
