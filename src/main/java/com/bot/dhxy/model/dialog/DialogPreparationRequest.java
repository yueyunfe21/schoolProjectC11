package com.bot.dhxy.model.dialog;

import com.bot.dhxy.service.dialog.DialogOperation;
import lombok.Builder;
import lombok.Value;

/**
 * Per-window request for the background watcher to prepare a dialog action.
 *
 * <p>This object only declares what the task expects. It never means the dialog was clicked or that
 * a task phase may advance.</p>
 */
@Value
@Builder
public class DialogPreparationRequest {
    DialogOperation operation;
    String targetKeyword;
    String source;
    String fromMap;
    Integer rememberedRelativeX;
    Integer rememberedRelativeY;
    String rememberedOptionText;
    long createdAtMs;
    long expiresAtMs;

    public boolean isExpired(long nowMs) {
        return expiresAtMs > 0 && nowMs > expiresAtMs;
    }
}
