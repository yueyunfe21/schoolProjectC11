package com.bot.dhxy.window.model;

import com.bot.dhxy.model.dialog.DialogType;
import lombok.Builder;
import lombok.Value;

/**
 * Window-level fact that the background watcher currently sees a dialog frame.
 *
 * <p>This model intentionally stores only observation data: which bound window saw which
 * high-level dialog shape and when. Business meaning such as route target, task option, or
 * prepared click point belongs in dialog preparation request/status/action models.</p>
 */
@Value
@Builder
public class WindowDialogSnapshot {
    String windowId;
    Long hwnd;
    DialogType type;
    String source;
    long detectedAtMs;
    /**
     * Screen-absolute dialog rectangle when the detector exposes one. Null means the current
     * detector only returned the high-level dialog type.
     */
    int[] dialogRect;
    /**
     * Screenshot provider used by the detector, for example HWND_PRINTWINDOW or ROBOT. Null means
     * the current detector path did not expose provider metadata.
     */
    String captureProvider;
}
