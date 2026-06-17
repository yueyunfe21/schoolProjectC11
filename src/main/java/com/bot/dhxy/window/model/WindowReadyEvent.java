package com.bot.dhxy.window.model;

import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import lombok.Builder;
import lombok.Value;

/**
 * Soft notification emitted by a window observer after it has refreshed per-window state.
 *
 * <p>This object is intentionally a wake hint only. It records which window changed and which
 * snapshot/task produced the signal, but consumers must re-read {@link WindowPathingSnapshot} or
 * other runtime state before sending input or advancing business phases.</p>
 */
@Value
@Builder(toBuilder = true)
public class WindowReadyEvent {
    String windowId;
    String hwnd;
    WindowReadyEventType type;
    TaskType taskType;
    String source;
    DialogOperation operation;
    String targetKeyword;
    WindowPathingState pathingState;
    WindowPathingIntent pathingIntent;
    WindowPathingSnapshot pathingSnapshot;
    @Builder.Default
    long createdAtMs = System.currentTimeMillis();
    long sequence;

    public String eventKey() {
        return windowId + ":" + type;
    }
}
