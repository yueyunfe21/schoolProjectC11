package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Route-dialog option click waiting for watcher confirmation before it becomes reusable memory.
 *
 * @param fromMap map observed before clicking the route option.
 * @param fromX logical X on {@code fromMap}; nullable when the watcher could not read it.
 * @param fromY logical Y on {@code fromMap}; nullable when the watcher could not read it.
 * @param targetMap map that must be confirmed by the pathing watcher before recording success.
 * @param relativeX dialog-relative X clicked inside the route option dialog.
 * @param relativeY dialog-relative Y clicked inside the route option dialog.
 * @param optionText OCR/remembered option text associated with the clicked option; nullable.
 * @param source diagnostic source that created this pending memory.
 * @param createdAtMs wall-clock timestamp when the click was submitted.
 */
@Value
@Builder
public class PendingTransferChoiceMemory {
    String fromMap;
    Integer fromX;
    Integer fromY;
    String targetMap;
    Integer relativeX;
    Integer relativeY;
    String optionText;
    String source;
    long createdAtMs;
}
