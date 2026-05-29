package com.bot.dhxy.model.dialog;

import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import lombok.Builder;
import lombok.Value;

/**
 * Structured result for one scoped dialog handling request.
 *
 * @param kind broad result category for task-side branching.
 * @param status stable service-level status.
 * @param dialogType dialog frame type seen by the handler; {@link DialogType#NONE} means no
 *                   verified dialog.
 * @param actionKey stable action key such as {@code xiuluo.acceptTask}; null when no action matched.
 * @param objective objective text payload parsed from a story/task dialog; null for action/no-dialog results.
 * @param matchedText OCR text or template path that produced the result, if available.
 * @param clicked whether the service sent a click.
 * @param relativeX clicked X relative to the dialog rectangle; null when no concrete point clicked.
 * @param relativeY clicked Y relative to the dialog rectangle; null when no concrete point clicked.
 * @param absoluteX screen-absolute clicked X; null when no concrete point clicked.
 * @param absoluteY screen-absolute clicked Y; null when no concrete point clicked.
 */
@Value
@Builder
public class DialogResult {
    DialogResultKind kind;
    DialogResultStatus status;
    DialogType dialogType;
    String actionKey;
    ObjectiveTextResult objective;
    String matchedText;
    boolean clicked;
    Integer relativeX;
    Integer relativeY;
    Integer absoluteX;
    Integer absoluteY;

    public static DialogResult simple(DialogResultStatus status, DialogType dialogType) {
        return statusBuilder(status, dialogType)
                .build();
    }

    public static DialogResultBuilder statusBuilder(DialogResultStatus status, DialogType dialogType) {
        return DialogResult.builder()
                .kind(kindFor(status))
                .status(status)
                .dialogType(dialogType)
                .clicked(isClickedStatus(status));
    }

    private static DialogResultKind kindFor(DialogResultStatus status) {
        if (status == DialogResultStatus.NO_DIALOG) {
            return DialogResultKind.NO_DIALOG;
        }
        if (status == DialogResultStatus.FAILED
                || status == DialogResultStatus.INTERRUPTED
                || status == DialogResultStatus.STORY_OBJECTIVE_NOT_FOUND
                || status == DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND
                || status == DialogResultStatus.OPTION_KEYWORD_NOT_FOUND
                || status == DialogResultStatus.BUSINESS_OPTION_NOT_FOUND
                || status == DialogResultStatus.GIVE_OPTION_NOT_FOUND
                || status == DialogResultStatus.GIVE_ITEM_FAILED) {
            return DialogResultKind.FAILED;
        }
        if (isClickedStatus(status)) {
            return DialogResultKind.ACTION;
        }
        if (status == DialogResultStatus.STORY_OBJECTIVE_READ) {
            return DialogResultKind.TEXT;
        }
        return DialogResultKind.UNKNOWN;
    }

    private static boolean isClickedStatus(DialogResultStatus status) {
        return status == DialogResultStatus.STORY_CLICKED
                || status == DialogResultStatus.OPTION_KEYWORD_CLICKED
                || status == DialogResultStatus.BUSINESS_OPTION_CLICKED
                || status == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || status == DialogResultStatus.FALLBACK_CLICKED;
    }
}
