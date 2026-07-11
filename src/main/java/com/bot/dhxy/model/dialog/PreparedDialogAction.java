package com.bot.dhxy.model.dialog;

import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelSourceType;
import lombok.Builder;
import lombok.Value;

/**
 * Per-window prepared dialog click candidate built by background observation.
 *
 * <p>The object stores a screen-absolute click point plus a small validation rectangle. A watcher
 * may refresh {@code lastVerifiedAtMs} after the validation fingerprint still matches; task code may
 * then click the cached point without rerunning full OCR/template matching.</p>
 *
 * @param windowId owning window runtime id; null only for standalone debug usage.
 * @param hwnd native window handle string when available.
 * @param intentId route/pathing intent id this action was prepared for; null for non-route actions
 *                 or legacy debug actions.
 * @param dialogType detected dialog frame type.
 * @param operation business operation this prepared action answers, such as route transfer.
 * @param targetKeyword expected target keyword for operation matching; null for operation-only
 *                      actions.
 * @param matchedText OCR text/template label that produced the click.
 * @param relativeX click X relative to the captured dialog rectangle.
 * @param relativeY click Y relative to the captured dialog rectangle.
 * @param absoluteX screen-absolute click X.
 * @param absoluteY screen-absolute click Y.
 * @param validationLeft screen-absolute validation crop left.
 * @param validationTop screen-absolute validation crop top.
 * @param validationRight screen-absolute validation crop right.
 * @param validationBottom screen-absolute validation crop bottom.
 * @param washMode color-cleaning mode used before fingerprinting the validation crop.
 * @param fingerprint binary fingerprint string for the cleaned validation crop.
 * @param clickRequired whether consuming task code should click {@code absoluteX/absoluteY}; false
 *                      for prepared story signals that only report visible text.
 * @param preparedAtMs epoch millis when full OCR/template matching prepared this action.
 * @param lastVerifiedAtMs epoch millis when watcher last confirmed the fingerprint still matched.
 * @param source source task/log label.
 * @param debugImagePath optional window-scoped image path used to prepare the action.
 * @param trackerPanelSourceType reader source for cached task-tracker clicks; local for normal dialogs.
 */
@Value
@Builder(toBuilder = true)
public class PreparedDialogAction {
    String windowId;
    String hwnd;
    String intentId;
    DialogType dialogType;
    DialogOperation operation;
    String targetKeyword;
    String matchedText;
    int relativeX;
    int relativeY;
    int absoluteX;
    int absoluteY;
    int validationLeft;
    int validationTop;
    int validationRight;
    int validationBottom;
    DialogFingerprintWashMode washMode;
    String fingerprint;
    @Builder.Default
    boolean clickRequired = true;
    long preparedAtMs;
    long lastVerifiedAtMs;
    String source;
    String debugImagePath;
    @Builder.Default
    TaskTrackerPanelSourceType trackerPanelSourceType = TaskTrackerPanelSourceType.LOCAL;

    public boolean matches(DialogOperation expectedOperation, String expectedKeyword) {
        if (operation != expectedOperation) {
            return false;
        }
        if (expectedKeyword == null || expectedKeyword.isBlank()) {
            return true;
        }
        return targetKeyword != null && targetKeyword.equals(expectedKeyword);
    }

    public boolean verifiedWithin(long nowMs, long maxAgeMs) {
        return lastVerifiedAtMs > 0 && maxAgeMs >= 0 && nowMs - lastVerifiedAtMs <= maxAgeMs;
    }
}
